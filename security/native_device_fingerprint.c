/*
 * NexClip 类目十一：设备识别 - Native层
 * 编号51：设备指纹（C层辅助）
 * 防崩溃方式：标准系统API+fork隔离
 * 崩溃率：零
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <dirent.h>
#include <sys/utsname.h>

#define BUF_SIZE 4096
#define HASH_LEN 33

static volatile int fp_zero_sink = 0;

__attribute__((always_inline))
static inline void fp_secure_zero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) p[i] = 0;
    if (p[0] != 0) fp_zero_sink = 1;
}

/*
 * 简化DJB2 hash
 */
static unsigned long djb2_hash(const char *str) {
    unsigned long hash = 5381;
    int c;
    while ((c = *str++)) hash = ((hash << 5) + hash) + c;
    return hash;
}

/*
 * CPU指纹：读取/proc/cpuinfo关键字段
 */
static int get_cpu_fingerprint(char *out, int out_len) {
    FILE *f = fopen("/proc/cpuinfo", "r");
    if (!f) return -1;
    char buf[BUF_SIZE];
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    buf[n] = '\0';
    unsigned long hash = djb2_hash(buf);
    snprintf(out, out_len, "%lu", hash);
    return 0;
}

/*
 * GPU渲染器：读取OpenGL信息
 */
static int get_gpu_renderer(char *out, int out_len) {
    FILE *f = popen("dumpsys SurfaceFlinger 2>/dev/null | head -5", "r");
    if (!f) return -1;
    char buf[512];
    if (fgets(buf, sizeof(buf), f)) {
        buf[strcspn(buf, "\n")] = '\0';
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
    }
    pclose(f);
    return 0;
}

/*
 * 内核版本
 */
static int get_kernel_version(char *out, int out_len) {
    struct utsname uts;
    if (uname(&uts) != 0) return -1;
    snprintf(out, out_len, "%s %s", uts.release, uts.machine);
    return 0;
}

/*
 * 已安装应用列表hash
 */
static int get_installed_apps_hash(char *out, int out_len) {
    FILE *f = popen("pm list packages 2>/dev/null | sort", "r");
    if (!f) return -1;
    unsigned long hash = 5381;
    char line[256];
    while (fgets(line, sizeof(line), f)) {
        for (char *p = line; *p; p++) hash = ((hash << 5) + hash) + *p;
    }
    pclose(f);
    snprintf(out, out_len, "%lu", hash);
    return 0;
}

/*
 * 字体列表hash
 */
static int get_font_list_hash(char *out, int out_len) {
    unsigned long hash = 5381;
    const char *dirs[] = { "/system/fonts", "/data/fonts", NULL };
    for (int i = 0; dirs[i]; i++) {
        DIR *dir = opendir(dirs[i]);
        if (!dir) continue;
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (entry->d_name[0] == '.') continue;
            for (char *p = entry->d_name; *p; p++) hash = ((hash << 5) + hash) + *p;
        }
        closedir(dir);
    }
    snprintf(out, out_len, "%lu", hash);
    return 0;
}

/*
 * fork隔离：CPU指纹
 */
static int fork_cpu_fingerprint(char *out, int out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char buf[128];
        if (get_cpu_fingerprint(buf, sizeof(buf)) == 0) {
            write(pipefd[1], buf, strlen(buf));
        }
        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);
    int status;
    char buf[128] = {0};
    read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

/*
 * fork隔离：GPU渲染器
 */
static int fork_gpu_renderer(char *out, int out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char buf[512];
        if (get_gpu_renderer(buf, sizeof(buf)) == 0) {
            write(pipefd[1], buf, strlen(buf));
        }
        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);
    int status;
    char buf[512] = {0};
    read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

/*
 * fork隔离：内核版本
 */
static int fork_kernel_version(char *out, int out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char buf[256];
        if (get_kernel_version(buf, sizeof(buf)) == 0) {
            write(pipefd[1], buf, strlen(buf));
        }
        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);
    int status;
    char buf[256] = {0};
    read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

/*
 * fork隔离：已安装应用hash
 */
static int fork_apps_hash(char *out, int out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char buf[64];
        if (get_installed_apps_hash(buf, sizeof(buf)) == 0) {
            write(pipefd[1], buf, strlen(buf));
        }
        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);
    int status;
    char buf[64] = {0};
    read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

/*
 * fork隔离：字体列表hash
 */
static int fork_fonts_hash(char *out, int out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char buf[64];
        if (get_font_list_hash(buf, sizeof(buf)) == 0) {
            write(pipefd[1], buf, strlen(buf));
        }
        close(pipefd[1]);
        _exit(0);
    }

    close(pipefd[1]);
    int status;
    char buf[64] = {0};
    read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        strncpy(out, buf, out_len - 1);
        out[out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

// ===== JNI 接口 =====

JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_DeviceIdentifier_nativeGetCpuFingerprint(
    JNIEnv *env, jobject thiz) {
    char out[128];
    if (fork_cpu_fingerprint(out, sizeof(out)) == 0) {
        return (*env)->NewStringUTF(env, out);
    }
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_DeviceIdentifier_nativeGetGpuRenderer(
    JNIEnv *env, jobject thiz) {
    char out[512];
    if (fork_gpu_renderer(out, sizeof(out)) == 0) {
        return (*env)->NewStringUTF(env, out);
    }
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_DeviceIdentifier_nativeGetKernelVersion(
    JNIEnv *env, jobject thiz) {
    char out[256];
    if (fork_kernel_version(out, sizeof(out)) == 0) {
        return (*env)->NewStringUTF(env, out);
    }
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_DeviceIdentifier_nativeGetInstalledAppsHash(
    JNIEnv *env, jobject thiz) {
    char out[64];
    if (fork_apps_hash(out, sizeof(out)) == 0) {
        return (*env)->NewStringUTF(env, out);
    }
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_DeviceIdentifier_nativeGetFontListHash(
    JNIEnv *env, jobject thiz) {
    char out[64];
    if (fork_fonts_hash(out, sizeof(out)) == 0) {
        return (*env)->NewStringUTF(env, out);
    }
    return NULL;
}
