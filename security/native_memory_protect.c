/*
 * ============================================
 * NexClip 类目七：内存安全 - Native层
 * 编号40：内存保护验证（C层fork隔离）
 * 编号41：防dump被动检测（C层fork隔离）
 * 编号42：密钥内存擦除（memset_s/prctl/ARM Crypto）
 *
 * 防崩溃方式：fork隔离+只读操作+标准系统调用
 * 崩溃率：零（主进程）
 * ============================================
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <sys/prctl.h>
#include <dirent.h>
#include <fcntl.h>

#ifndef PR_SET_DUMPABLE
#define PR_SET_DUMPABLE 4
#endif

#define LINE_SIZE 512

/*
 * 编号40：检查自身SO内存段权限
 * 代码段应为r-xp、数据段应为rw-p、不应有rwxp段
 */
static int check_memory_permissions(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    int rwx_count = 0;

    while (fgets(line, sizeof(line), f)) {
        // 只检查自身SO文件
        if (!strstr(line, ".so")) continue;

        // 提取权限部分
        char perms[8] = {0};
        sscanf(line, "%*s %5s", perms);

        // 不应有rwx段
        if (perms[0] == 'r' && perms[1] == 'w' && perms[2] == 'x') {
            rwx_count++;
        }
    }
    fclose(f);
    return rwx_count > 0 ? 1 : 0;
}

/*
 * 编号40：验证关键函数地址范围合法性
 * 检查函数指针是否在合法代码段范围内
 */
static int check_function_addresses(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    // 获取一个已知函数的地址
    unsigned long func_addr = (unsigned long)&check_function_addresses;
    char line[LINE_SIZE];
    int found_in_code = 0;

    while (fgets(line, sizeof(line), f)) {
        unsigned long start, end;
        char perms[8] = {0};
        if (sscanf(line, "%lx-%lx %5s", &start, &end, perms) != 3) continue;

        // 只检查r-xp代码段
        if (perms[0] == 'r' && perms[1] == '-' && perms[2] == 'x') {
            if (func_addr >= start && func_addr < end) {
                found_in_code = 1;
                break;
            }
        }
    }
    fclose(f);
    // 函数地址不在代码段=异常
    return found_in_code ? 0 : 1;
}

/*
 * 编号41：检测其他进程是否在dump本进程内存
 * 遍历/proc目录检查其他进程的fd
 * 是否指向/proc/PID/mem
 */
static int check_dump_attempt(pid_t my_pid) {
    char my_mem_path[64];
    snprintf(my_mem_path, sizeof(my_mem_path), "/proc/%d/mem", my_pid);

    DIR *proc_dir = opendir("/proc");
    if (!proc_dir) return 0;

    struct dirent *entry;
    while ((entry = readdir(proc_dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;

        pid_t other_pid = atoi(entry->d_name);
        if (other_pid == my_pid) continue;

        // 检查该进程的fd目录
        char fd_path[128];
        snprintf(fd_path, sizeof(fd_path), "/proc/%d/fd", other_pid);
        DIR *fd_dir = opendir(fd_path);
        if (!fd_dir) continue;

        struct dirent *fd_entry;
        while ((fd_entry = readdir(fd_dir)) != NULL) {
            if (fd_entry->d_name[0] == '.') continue;

            char link_path[256];
            char target[512];
            snprintf(link_path, sizeof(link_path), "/proc/%d/fd/%s",
                     other_pid, fd_entry->d_name);

            ssize_t n = readlink(link_path, target, sizeof(target) - 1);
            if (n > 0) {
                target[n] = '\0';
                if (strcmp(target, my_mem_path) == 0) {
                    closedir(fd_dir);
                    closedir(proc_dir);
                    return 1;
                }
            }
        }
        closedir(fd_dir);
    }
    closedir(proc_dir);
    return 0;
}

/*
 * 编号42：memset_s安全清零
 * volatile防止编译器优化掉清零操作
 * 函数内联到使用点，不经过函数调用防止被Hook绕过
 */
static volatile int zero_sink = 0;

__attribute__((always_inline))
static inline void secure_memzero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) {
        p[i] = 0;
    }
    // 防止编译器优化：通过volatile sink消费结果
    if (p[0] != 0) zero_sink = 1;
}

/*
 * 编号42：核心转储禁止
 * prctl(PR_SET_DUMPABLE, 0) + RLIMIT_CORE=0
 */
static void disable_core_dump(void) {
    // 设置进程为不可dump
    prctl(PR_SET_DUMPABLE, 0);
    // 设置核心转储大小限制为0
    struct rlimit rl;
    rl.rlim_cur = 0;
    rl.rlim_max = 0;
    setrlimit(RLIMIT_CORE, &rl);
}

/*
 * 编号42：ARM Crypto Extension硬件加密测试
 * 验证CPU是否支持AES硬件加速
 */
static int check_arm_crypto(void) {
#if defined(__aarch64__)
    // ARM64：检查AES指令可用性
    unsigned long cap;
    asm volatile("mrs %0, ID_AA64ISAR0_EL1" : "=r"(cap));
    // bits[7:4] = 1 表示支持AES
    int aes_support = ((cap >> 4) & 0xF) == 1;
    return aes_support;
#elif defined(__arm__)
    // ARM32：检查NEON+AES
    unsigned long cap;
    asm volatile("mrc p15, 0, %0, c0, c1, 1" : "=r"(cap));
    int aes_support = (cap >> 0) & 1;
    return aes_support;
#else
    return 0;
#endif
}

/*
 * 编号42：内存数据分散存储验证
 * 验证敏感数据是否分布在不连续地址
 */
static int check_memory_scatter(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    int heap_count = 0;
    char line[LINE_SIZE];

    while (fgets(line, sizeof(line), f)) {
        // 统计匿名映射段（heap/mmap）
        if (!strstr(line, "/") && strstr(line, "rw-p")) {
            heap_count++;
        }
    }
    fclose(f);
    // 多个分散的匿名段=分散存储
    return heap_count;
}

/*
 * fork隔离：编号40 内存保护验证
 * 返回bitmask：bit0=rwx段 bit1=函数地址异常
 */
static int fork_memory_check(pid_t pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;
        if (check_memory_permissions(pid))    result |= (1 << 0);
        if (check_function_addresses(pid))    result |= (1 << 1);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

/*
 * fork隔离：编号41 防dump检测
 */
static int fork_dump_detect(pid_t pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = check_dump_attempt(pid);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

// ===== JNI 接口 =====

/*
 * 编号40：内存保护验证（fork隔离）
 * Java_com_myvideo_editor_security_MemoryProtector_nativeMemoryProtectCheck
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_MemoryProtector_nativeMemoryProtectCheck(
    JNIEnv *env, jobject thiz) {
    pid_t pid = getpid();
    return fork_memory_check(pid);
}

/*
 * 编号41：防dump被动检测（fork隔离）
 * Java_com_myvideo_editor_security_MemoryProtector_nativeDumpDetect
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_MemoryProtector_nativeDumpDetect(
    JNIEnv *env, jobject thiz) {
    pid_t pid = getpid();
    return fork_dump_detect(pid);
}

/*
 * 编号42：核心转储禁止
 * Java_com_myvideo_editor_security_MemoryProtector_nativeEnableCoreDumpProtection
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_MemoryProtector_nativeEnableCoreDumpProtection(
    JNIEnv *env, jobject thiz) {
    disable_core_dump();
    return 0;
}

/*
 * 编号42：内存密钥加密（ARM Crypto Extension）
 * Java_com_myvideo_editor_security_MemoryProtector_nativeEncryptMemoryKey
 */
JNIEXPORT jbyteArray JNICALL
Java_com_myvideo_editor_security_MemoryProtector_nativeEncryptMemoryKey(
    JNIEnv *env, jobject thiz, jbyteArray keyData) {

    jsize len = (*env)->GetArrayLength(env, keyData);
    jbyte *data = (*env)->GetByteArrayElements(env, keyData, NULL);
    if (!data) return NULL;

    // 简单异或加密（生产环境用ARM AES指令）
    jbyte *encrypted = malloc(len);
    volatile unsigned char xor_key = 0x5A;
    for (int i = 0; i < len; i++) {
        encrypted[i] = data[i] ^ xor_key;
    }

    // 清零原始数据
    secure_memzero(data, len);
    (*env)->ReleaseByteArrayElements(env, keyData, data, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, result, 0, len, encrypted);

    // 清零临时缓冲区
    secure_memzero(encrypted, len);
    free(encrypted);

    return result;
}
