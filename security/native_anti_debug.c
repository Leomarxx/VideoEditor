/*
 * ============================================
 * NexClip 类目三：反调试 - Native层
 * 编号18：多层调试器检测（C层部分）
 * 编号33：混合层级检测（C层fork隔离）
 *
 * 防崩溃方式：fork隔离
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
#include <fcntl.h>

/*
 * 编号18 层1：/proc/self/status TracerPid
 * 有调试器时TracerPid!=0
 */
static int check_tracer_pid(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/status", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[256];
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            int tracer_pid = atoi(line + 10);
            fclose(f);
            return tracer_pid != 0 ? 1 : 0;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号18 层2：/proc/self/wchan 包含ptrace/traced
 */
static int check_wchan(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/wchan", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char buf[256];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    buf[n] = '\0';

    if (strstr(buf, "ptrace") || strstr(buf, "traced")) {
        return 1;
    }
    return 0;
}

/*
 * 编号18 层3：JDWP端口检查
 * /proc/net/tcp中有jdwp关键字=有调试器
 */
static int check_jdwp(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char buf[1024];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);

    for (size_t i = 0; i < n; i++) {
        if (buf[i] == '\0') buf[i] = ' ';
    }
    buf[n] = '\0';

    return strstr(buf, "jdwp") != NULL ? 1 : 0;
}

/*
 * 编号18 层8：/proc/self/task中每个线程TracerPid
 */
static int check_task_tracer(pid_t pid) {
    char path[128];
    snprintf(path, sizeof(path), "/proc/%d/task", pid);
    DIR *dir = opendir(path);
    if (!dir) return 0;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;

        char status_path[256];
        snprintf(status_path, sizeof(status_path),
                 "/proc/%d/task/%s/status", pid, entry->d_name);
        FILE *f = fopen(status_path, "r");
        if (!f) continue;

        char line[256];
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "TracerPid:", 10) == 0) {
                int tp = atoi(line + 10);
                fclose(f);
                if (tp != 0) { closedir(dir); return 1; }
            }
        }
        fclose(f);
    }
    closedir(dir);
    return 0;
}

#include <dirent.h>

/*
 * 编号33 C层检测1：/proc/self/fdinfo中调试器fd
 * 正常APP不应有调试相关fd
 */
static int check_fdinfo(pid_t pid) {
    char path[128];
    snprintf(path, sizeof(path), "/proc/%d/fd", pid);
    DIR *dir = opendir(path);
    if (!dir) return 0;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;

        char fdinfo_path[256];
        snprintf(fdinfo_path, sizeof(fdinfo_path),
                 "/proc/%d/fdinfo/%s", pid, entry->d_name);
        FILE *f = fopen(fdinfo_path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        if (strstr(buf, "trace") || strstr(buf, "debug")) {
            closedir(dir);
            return 1;
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号33 C层检测2：/proc/self/exe符号链接目标
 * 正常APP链接到APK路径
 */
static int check_exe_link(pid_t pid) {
    char path[64];
    char target[512];
    snprintf(path, sizeof(path), "/proc/%d/exe", pid);

    ssize_t n = readlink(path, target, sizeof(target) - 1);
    if (n < 0) return 0;
    target[n] = '\0';

    if (strstr(target, "frida") || strstr(target, "inject") ||
        strstr(target, "debug") || strstr(target, "hook")) {
        return 1;
    }
    return 0;
}

/*
 * 编号33 C层检测3：LD_PRELOAD环境变量检查
 * 非空=有SO被预注入
 */
static int check_ld_preload(void) {
    const char *preload = getenv("LD_PRELOAD");
    if (preload && strlen(preload) > 0) {
        return 1;
    }
    return 0;
}

/*
 * fork隔离执行全部C层检测
 * 子进程崩溃不影响主进程
 * 返回：0=正常，非0=异常（bitmask）
 *   bit0: TracerPid
 *   bit1: Wchan
 *   bit2: JDWP
 *   bit3: TaskTracer
 *   bit4: fdinfo
 *   bit5: exe_link
 *   bit6: LD_PRELOAD
 */
static int fork_detect(pid_t pid) {
    pid_t child = fork();

    if (child < 0) {
        return 0;
    }

    if (child == 0) {
        int result = 0;
        if (check_tracer_pid(pid))  result |= (1 << 0);
        if (check_wchan(pid))       result |= (1 << 1);
        if (check_jdwp(pid))        result |= (1 << 2);
        if (check_task_tracer(pid)) result |= (1 << 3);
        if (check_fdinfo(pid))      result |= (1 << 4);
        if (check_exe_link(pid))    result |= (1 << 5);
        if (check_ld_preload())     result |= (1 << 6);
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
 * 编号18+33：C层检测（fork隔离）
 * Java_com_myvideo_editor_security_DebuggerDetector_nativeAntiDebugDetect
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_DebuggerDetector_nativeAntiDebugDetect(
    JNIEnv *env, jobject thiz) {

    pid_t pid = getpid();
    return fork_detect(pid);
}
