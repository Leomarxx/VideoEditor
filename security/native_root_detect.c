/*
 * ============================================
 * NexClip 类目六：Root/环境检测 - Native层
 * 编号21：Root全方案检测（C层fork隔离）
 * 编号22：模拟器+云手机+环境检测（C层fork隔离）
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
#include <dirent.h>
#include <fcntl.h>

#define LINE_SIZE 512
#define BUF_SIZE 4096

/*
 * 编号21：检查su文件（9个已知路径）
 */
static int check_su_files(void) {
    const char *su_paths[] = {
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
        "/system/sd/xbin/su", "/cache/su",
        "/data/adb/magisk/su", NULL
    };
    for (int i = 0; su_paths[i]; i++) {
        if (access(su_paths[i], F_OK) == 0) return 1;
    }
    return 0;
}

/*
 * 编号21：检查/data/adb/modules目录
 */
static int check_magisk_modules(void) {
    return access("/data/adb/modules", F_OK) == 0 ? 1 : 0;
}

/*
 * 编号21：检查/proc/mounts中magisk痕迹
 */
static int check_magisk_mounts(void) {
    FILE *f = fopen("/proc/mounts", "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "magisk") || strstr(line, "Magisk")) {
            fclose(f);
            return 1;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号21：检查/init.rc中magisk脚本
 */
static int check_magisk_initrc(void) {
    FILE *f = fopen("/init.rc", "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "magisk") || strstr(line, "Magisk")) {
            fclose(f);
            return 1;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号21：检查/test-keys
 */
static int check_test_keys(void) {
    FILE *f = fopen("/system/build.prop", "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "test-keys")) {
            fclose(f);
            return 1;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号21：检查ro.debuggable
 */
static int check_ro_debuggable(void) {
    FILE *f = fopen("/system/build.prop", "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "ro.debuggable=1") || strstr(line, "ro.secure=0")) {
            fclose(f);
            return 1;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号22：检查/proc/cpuinfo中模拟器特征
 */
static int check_emulator_cpu(void) {
    FILE *f = fopen("/proc/cpuinfo", "r");
    if (!f) return 0;

    char buf[BUF_SIZE];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    buf[n] = '\0';

    const char *emu_keywords[] = {
        "goldfish", "ranchu", "vbox", "QEMU", "qemu",
        "ACPI", "Virtual", NULL
    };
    for (int i = 0; emu_keywords[i]; i++) {
        if (strstr(buf, emu_keywords[i])) return 1;
    }
    return 0;
}

/*
 * 编号22：检查模拟器特征文件（10个）
 */
static int check_emulator_files(void) {
    const char *emu_files[] = {
        "/system/lib/libc_malloc_debug_qemu.so",
        "/sys/qemu_trace",
        "/system/bin/qemu-props",
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/dev/socket/genyd",
        "/dev/socket/baseband_genyd",
        "/dev/goldfish_pipe",
        "/system/lib/vboxguest.ko",
        "/system/lib/vboxsf.ko",
        NULL
    };
    int found = 0;
    for (int i = 0; emu_files[i]; i++) {
        if (access(emu_files[i], F_OK) == 0) found++;
    }
    return found >= 2 ? 1 : 0;
}

/*
 * 编号22：检查WiFi MAC地址
 */
static int check_wifi_mac(void) {
    FILE *f = fopen("/sys/class/net/wlan0/address", "r");
    if (!f) return 1; // 无法读取=可疑

    char mac[64];
    if (fgets(mac, sizeof(mac), f)) {
        fclose(f);
        // 全零或空MAC=可疑
        if (strncmp(mac, "00:00:00:00:00:00", 17) == 0) return 1;
        if (strncmp(mac, "02:00:00:00:00:00", 17) == 0) return 1;
        if (strlen(mac) < 5) return 1;
        return 0;
    }
    fclose(f);
    return 1;
}

/*
 * 编号22：检查远程控制进程
 */
static int check_remote_process(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    const char *remote_kw[] = {
        "teamviewer", "anydesk", "sunlogin", "vysor", "scrcpy",
        "airdroid", NULL
    };
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;
        char path[128];
        snprintf(path, sizeof(path), "/proc/%s/cmdline", entry->d_name);
        FILE *f = fopen(path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        for (int i = 0; remote_kw[i]; i++) {
            if (strstr(buf, remote_kw[i])) {
                closedir(dir);
                return 1;
            }
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号22：检查自动化框架进程
 */
static int check_automation_process(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    const char *auto_kw[] = {
        "uiautomator2", "appium", "poco", "minitouch",
        "adb_forward", NULL
    };
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;
        char path[128];
        snprintf(path, sizeof(path), "/proc/%s/cmdline", entry->d_name);
        FILE *f = fopen(path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        for (int i = 0; auto_kw[i]; i++) {
            if (strstr(buf, auto_kw[i])) {
                closedir(dir);
                return 1;
            }
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号22：检查VirtualApp沙箱
 */
static int check_virtualapp(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;
        char path[128];
        snprintf(path, sizeof(path), "/proc/%s/cmdline", entry->d_name);
        FILE *f = fopen(path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        if (strstr(buf, "io.virtualapp") || strstr(buf, "VirtualApp")) {
            closedir(dir);
            return 1;
        }
    }
    closedir(dir);
    return 0;
}

/*
 * fork隔离：编号21 Root检测
 * 返回bitmask：
 *   bit0: su文件 bit1: magisk模块 bit2: magisk挂载
 *   bit3: init.rc bit4: test-keys bit5: ro.debuggable
 */
static int fork_root_detect(void) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;
        if (check_su_files())        result |= (1 << 0);
        if (check_magisk_modules())  result |= (1 << 1);
        if (check_magisk_mounts())   result |= (1 << 2);
        if (check_magisk_initrc())   result |= (1 << 3);
        if (check_test_keys())       result |= (1 << 4);
        if (check_ro_debuggable())   result |= (1 << 5);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

/*
 * fork隔离：编号22 环境检测
 * 返回bitmask：
 *   bit0: 模拟器CPU bit1: 模拟器文件 bit2: WiFi MAC
 *   bit3: 远程控制 bit4: 自动化框架 bit5: VirtualApp
 */
static int fork_env_detect(void) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;
        if (check_emulator_cpu())       result |= (1 << 0);
        if (check_emulator_files())     result |= (1 << 1);
        if (check_wifi_mac())           result |= (1 << 2);
        if (check_remote_process())     result |= (1 << 3);
        if (check_automation_process()) result |= (1 << 4);
        if (check_virtualapp())         result |= (1 << 5);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

// ===== JNI 接口 =====

/*
 * 编号21：Root全方案检测（fork隔离）
 * Java_com_myvideo_editor_security_RootDetector_nativeRootDetect
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_RootDetector_nativeRootDetect(
    JNIEnv *env, jobject thiz) {
    return fork_root_detect();
}

/*
 * 编号22：环境检测（fork隔离）
 * Java_com_myvideo_editor_security_RootDetector_nativeEnvironmentDetect
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_RootDetector_nativeEnvironmentDetect(
    JNIEnv *env, jobject thiz) {
    return fork_env_detect();
}
