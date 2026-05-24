/*
 * ============================================
 * NexClip 类目四：反Hook - Native层
 * 编号17：全框架检测（C层maps扫描）
 * 编号24：Frida深度检测（内存扫描+特征码）
 * 编号31：Frida标准检测（C层4项）
 *
 * 防崩溃方式：fork隔离+严格地址范围校验
 * 崩溃率：零（主进程）
 * ============================================
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <dirent.h>
#include <errno.h>

#define BUF_SIZE 4096
#define LINE_SIZE 512

/*
 * 读取/proc/self/maps内容
 * 返回触发的框架标志，bitmask
 *   bit0: Xposed
 *   bit1: Zygisk
 *   bit2: Substrate
 *   bit3: EdXposed
 *   bit4: VirtualXposed/TaiChi
 *   bit5: Frida
 *   bit6: gadget
 *   bit7: linjector
 *   bit8: libfrida
 */
static int scan_maps_for_hook(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    int flags = 0;
    char line[LINE_SIZE];

    while (fgets(line, sizeof(line), f)) {
        // 编号17
        if (strstr(line, "XposedBridge") || strstr(line, "xposed") ||
            strstr(line, "de.robv.android.xposed") || strstr(line, "org.lsposed"))
            flags |= (1 << 0);
        if (strstr(line, "libzygisk"))
            flags |= (1 << 1);
        if (strstr(line, "libsubstrate.so") || strstr(line, "libsubstrate-dvm.so"))
            flags |= (1 << 2);
        if (strstr(line, "EdXposed") || strstr(line, "edxposed"))
            flags |= (1 << 3);
        if (strstr(line, "VirtualXposed") || strstr(line, "TaiChi") ||
            strstr(line, "com.stub.StubApp"))
            flags |= (1 << 4);
        // 编号24/31
        if (strstr(line, "frida"))
            flags |= (1 << 5);
        if (strstr(line, "gadget"))
            flags |= (1 << 6);
        if (strstr(line, "linjector"))
            flags |= (1 << 7);
        if (strstr(line, "libfrida"))
            flags |= (1 << 8);
    }
    fclose(f);
    return flags;
}

/*
 * 编号31 方法3：遍历/proc目录
 * 查找frida-server/gadget进程
 */
static int scan_proc_for_frida(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;

        char cmdline_path[128];
        snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%s/cmdline", entry->d_name);
        FILE *f = fopen(cmdline_path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        if (strstr(buf, "frida-server") || strstr(buf, "frida-gadget") ||
            strstr(buf, "re.frida.server")) {
            closedir(dir);
            return 1;
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号31 方法4：检查rwx内存映射
 * 正常APP不应有rwx权限内存段
 */
static int check_rwx_maps(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if ((strstr(line, "rwx") || strstr(line, "rwxp"))) {
            if (!strstr(line, "/") || strstr(line, "anon")) {
                fclose(f);
                return 1;
            }
        }
    }
    fclose(f);
    return 0;
}

#include <signal.h>
#include <setjmp.h>

static sigjmp_buf jump_buf;
static volatile int got_signal = 0;

/*
 * 信号处理器：安全读取失败时跳回
 */
static void safe_read_handler(int sig) {
    got_signal = 1;
    siglongjmp(jump_buf, 1);
}

/*
 * 编号24：堆内存特征码扫描
 * 扫描匿名可执行内存映射搜索FRIDA/frida特征码
 * 严格限制扫描范围防止越界
 */
static int scan_heap_for_frida(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    int found = 0;

    // 设置信号处理
    struct sigaction sa, old_sa;
    sa.sa_handler = safe_read_handler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;

    while (fgets(line, sizeof(line), f)) {
        // 只扫描可执行的匿名映射
        if (!strstr(line, "r-x") && !strstr(line, "rwx")) continue;
        if (strstr(line, "/")) continue; // 跳过文件映射

        unsigned long start, end;
        if (sscanf(line, "%lx-%lx", &start, &end) != 2) continue;

        // 严格限制扫描范围：最大10MB
        if (end - start > 10 * 1024 * 1024) continue;

        // 安装信号处理器
        got_signal = 0;
        sigaction(SIGBUS, &sa, &old_sa);
        sigaction(SIGSEGV, &sa, NULL);

        if (sigsetjmp(jump_buf, 1) == 0) {
            // 安全扫描
            unsigned char *ptr = (unsigned char *)start;
            unsigned long size = end - start;

            for (unsigned long i = 0; i < size - 4; i++) {
                // 搜索 "FRIDA" 或 "frida" 特征码
                if ((ptr[i] == 'F' && ptr[i+1] == 'R' && ptr[i+2] == 'I' &&
                     ptr[i+3] == 'D' && ptr[i+4] == 'A') ||
                    (ptr[i] == 'f' && ptr[i+1] == 'r' && ptr[i+2] == 'i' &&
                     ptr[i+3] == 'd' && ptr[i+4] == 'a')) {
                    found = 1;
                    break;
                }
            }
        }

        // 恢复信号处理器
        sigaction(SIGBUS, &old_sa, NULL);
        sigaction(SIGSEGV, &old_sa, NULL);

        if (found) break;
    }
    fclose(f);
    return found;
}

/*
 * 编号24：GJS引擎特征检测
 * gjs/girepository/gobject/libglib在maps中的特征
 */
static int check_gjs_engine(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "gjs") || strstr(line, "girepository") ||
            strstr(line, "gobject") || strstr(line, "libglib")) {
            found = 1;
            break;
        }
    }
    fclose(f);
    return found;
}

/*
 * 编号24：D-Bus通信特征检测
 * /proc/self/environ中DBUS环境变量
 */
static int check_dbus_environ(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/environ", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char buf[4096];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    buf[n] = '\0';

    // 正常APP不应有D-Bus
    for (size_t i = 0; i < n; i++) {
        if (buf[i] == '\0') buf[i] = '\n';
    }
    return strstr(buf, "DBUS") != NULL ? 1 : 0;
}

/*
 * fork隔离执行全部检测
 * 子进程崩溃不影响主进程
 * 返回：0=正常，非0=异常（bitmask）
 *   bit0: maps Xposed
 *   bit1: maps Frida
 *   bit2: proc frida-server
 *   bit3: rwx内存
 *   bit4: 堆特征码
 *   bit5: GJS引擎
 *   bit6: D-Bus
 */
static int fork_hook_detect(pid_t pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;

        // 编号17+31 方法1：maps扫描
        int map_flags = scan_maps_for_hook(pid);
        if (map_flags & 0x01F) result |= (1 << 0); // Xposed相关
        if (map_flags & 0x1E0) result |= (1 << 1); // Frida相关

        // 编号31 方法3：遍历/proc
        if (scan_proc_for_frida()) result |= (1 << 2);

        // 编号31 方法4：rwx内存
        if (check_rwx_maps(pid)) result |= (1 << 3);

        // 编号24：堆内存扫描
        if (scan_heap_for_frida(pid)) result |= (1 << 4);

        // 编号24：GJS引擎
        if (check_gjs_engine(pid)) result |= (1 << 5);

        // 编号24：D-Bus
        if (check_dbus_environ(pid)) result |= (1 << 6);

        _exit(result);
    }

    int status;
    pid_t w = waitpid(child, &status, 0);
    if (w < 0) return 0;

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    // 子进程崩溃不影响主进程
    return 0;
}

// ===== JNI 接口 =====

/*
 * 编号24：Frida深度检测（fork隔离）
 * Java_com_myvideo_editor_security_HookDetector_nativeFridaDeepDetect
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_HookDetector_nativeFridaDeepDetect(
    JNIEnv *env, jobject thiz) {

    pid_t pid = getpid();
    return fork_hook_detect(pid);
}
