/*
 * ============================================
 * NexClip 类目十：界面保护 - Native层
 * 编号49：组件安全（C层辅助检测）
 * 编号50：录屏检测（C层辅助检测）
 *
 * 防崩溃方式：标准系统API+fork隔离
 * 崩溃率：零
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

/*
 * 安全清零
 */
static volatile int ui_zero_sink = 0;

__attribute__((always_inline))
static inline void ui_secure_zero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) p[i] = 0;
    if (p[0] != 0) ui_zero_sink = 1;
}

/*
 * 编号49：检测可疑悬浮窗/覆盖层
 * 遍历/proc目录检查是否有未知进程在绘制overlay
 */
static int detect_overlay(pid_t my_pid) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    char my_path[64];
    snprintf(my_path, sizeof(my_path), "/proc/%d", my_pid);

    struct dirent *entry;
    const char *suspicious_kw[] = {
        "overlay", "float", "screen_record", "capture",
        "mirror", "cast", "vnc", "remote", NULL
    };

    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;

        pid_t other_pid = atoi(entry->d_name);
        if (other_pid == my_pid) continue;

        char cmdline_path[128];
        snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%d/cmdline", other_pid);

        FILE *f = fopen(cmdline_path, "r");
        if (!f) continue;

        char buf[512];
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        buf[n] = '\0';

        for (int i = 0; suspicious_kw[i]; i++) {
            if (strstr(buf, suspicious_kw[i])) {
                closedir(dir);
                return 1;
            }
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号50：检测屏幕录制/capture服务
 * 检查是否有录屏相关的服务在运行
 */
static int detect_screen_capture(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    const char *capture_kw[] = {
        "screenrecord", "screencapture", "mediaprojection",
        "screen_recorder", "azscreenrecorder", "mobizen",
        "du_recorder", "rec", "xrecorder", NULL
    };

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

        for (int i = 0; capture_kw[i]; i++) {
            if (strstr(buf, capture_kw[i])) {
                closedir(dir);
                return 1;
            }
        }
    }
    closedir(dir);
    return 0;
}

/*
 * 编号50：检测投屏相关服务
 * Miracast/WiDi/Chromecast
 */
static int detect_casting_service(void) {
    DIR *dir = opendir("/proc");
    if (!dir) return 0;

    struct dirent *entry;
    const char *cast_kw[] = {
        "miracast", "widi", "chromecast", "airplay",
        "screencast", "displaylink", "apowermirror",
        "letsview", NULL
    };

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

        for (int i = 0; cast_kw[i]; i++) {
            if (strstr(buf, cast_kw[i])) {
                closedir(dir);
                return 1;
            }
        }
    }
    closedir(dir);
    return 0;
}

/*
 * fork隔离：编号49 悬浮窗检测
 * 子进程检测，崩溃不影响主进程
 */
static int fork_overlay_detect(pid_t my_pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = detect_overlay(my_pid);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

/*
 * fork隔离：编号50 屏幕录制检测
 */
static int fork_screen_capture_detect(void) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = detect_screen_capture();
        if (result == 0) {
            result = detect_casting_service();
        }
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

// ===== JNI 接口 =====

/*
 * 编号49：检测可疑悬浮窗/覆盖层（fork隔离）
 * Java_com_myvideo_editor_security_UIProtector_nativeDetectOverlay
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_UIProtector_nativeDetectOverlay(
    JNIEnv *env, jobject thiz, jint pid) {

    int result = fork_overlay_detect((pid_t)pid);
    return result > 0 ? JNI_TRUE : JNI_FALSE;
}

/*
 * 编号50：检测屏幕录制/capture（fork隔离）
 * Java_com_myvideo_editor_security_UIProtector_nativeDetectScreenCapture
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_UIProtector_nativeDetectScreenCapture(
    JNIEnv *env, jobject thiz) {

    int result = fork_screen_capture_detect();
    return result > 0 ? JNI_TRUE : JNI_FALSE;
}
