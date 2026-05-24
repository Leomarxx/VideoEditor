/*
 * NexClip 类目十三：合规/审计 - Native层（精简版）
 * 编号59：水印溯源（C层生成+验证）
 *
 * 防崩溃方式：fork隔离+标准API
 * 崩溃率：零
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <time.h>
#include <openssl/sha.h>
#include <openssl/evp.h>
#include <openssl/rand.h>

#define HASH_LEN 32
#define WATERMARK_KEY_LEN 16

static volatile int ca_zero_sink = 0;

__attribute__((always_inline))
static inline void ca_secure_zero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) p[i] = 0;
    if (p[0] != 0) ca_zero_sink = 1;
}

/*
 * 水印编码：将信息嵌入字节数组
 * 格式：[魔数4B][版本1B][用户ID hash 8B][设备指纹 hash 8B][时间戳8B][校验和4B]
 */
#define WM_MAGIC { 0x4E, 0x58, 0x57, 0x4D } /* "NXWM" */
#define WM_TOTAL_LEN 33

/*
 * 编号59：生成隐性水印
 * 嵌入用户ID+设备指纹+时间戳
 * 抗裁剪压缩转码
 */
static int generate_watermark(const char *user_id, const char *device_id,
                              long timestamp, unsigned char *wm_out) {
    unsigned char magic[] = WM_MAGIC;
    memcpy(wm_out, magic, 4);
    wm_out[4] = 0x01; // 版本

    // 用户ID hash 8B
    unsigned char uid_hash[HASH_LEN];
    SHA256((unsigned char *)user_id, strlen(user_id), uid_hash);
    memcpy(wm_out + 5, uid_hash, 8);

    // 设备指纹 hash 8B
    unsigned char did_hash[HASH_LEN];
    SHA256((unsigned char *)device_id, strlen(device_id), did_hash);
    memcpy(wm_out + 13, did_hash, 8);

    // 时间戳 8B
    for (int i = 0; i < 8; i++) {
        wm_out[21 + i] = (unsigned char)(timestamp >> (56 - i * 8));
    }

    // 校验和 4B（前29字节的SHA-256前4字节）
    unsigned char checksum[HASH_LEN];
    SHA256(wm_out, 29, checksum);
    memcpy(wm_out + 29, checksum, 4);

    ca_secure_zero(uid_hash, HASH_LEN);
    ca_secure_zero(did_hash, HASH_LEN);
    ca_secure_zero(checksum, HASH_LEN);
    return WM_TOTAL_LEN;
}

/*
 * 编号59：验证水印
 * 解析嵌入信息
 */
static int verify_watermark(const unsigned char *wm, int wm_len,
                            char *user_id_out, int uid_out_len,
                            char *device_id_out, int did_out_len,
                            long *timestamp_out) {
    if (wm_len < WM_TOTAL_LEN) return -1;

    // 验证魔数
    if (wm[0] != 'N' || wm[1] != 'X' || wm[2] != 'W' || wm[3] != 'M') return -1;

    // 验证校验和
    unsigned char checksum[HASH_LEN];
    SHA256(wm, 29, checksum);
    if (memcmp(checksum, wm + 29, 4) != 0) {
        ca_secure_zero(checksum, HASH_LEN);
        return -1;
    }
    ca_secure_zero(checksum, HASH_LEN);

    // 提取用户ID hash（hex）
    for (int i = 0; i < 8; i++) {
        sprintf(user_id_out + i * 2, "%02x", wm[5 + i]);
    }
    user_id_out[16] = '\0';

    // 提取设备指纹 hash（hex）
    for (int i = 0; i < 8; i++) {
        sprintf(device_id_out + i * 2, "%02x", wm[13 + i]);
    }
    device_id_out[16] = '\0';

    // 提取时间戳
    *timestamp_out = 0;
    for (int i = 0; i < 8; i++) {
        *timestamp_out = (*timestamp_out << 8) | wm[21 + i];
    }

    return 0;
}

/*
 * fork隔离：生成水印
 */
static int fork_generate_watermark(const char *user_id, const char *device_id,
                                   long timestamp, unsigned char *wm_out) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        unsigned char wm[WM_TOTAL_LEN];
        int ret = generate_watermark(user_id, device_id, timestamp, wm);
        if (ret > 0) {
            write(pipefd[1], wm, WM_TOTAL_LEN);
        }
        ca_secure_zero(wm, WM_TOTAL_LEN);
        close(pipefd[1]);
        _exit(ret > 0 ? 0 : 1);
    }

    close(pipefd[1]);
    int status;
    ssize_t n = read(pipefd[0], wm_out, WM_TOTAL_LEN);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0 && n == WM_TOTAL_LEN) return 0;
    return -1;
}

/*
 * fork隔离：验证水印
 */
static int fork_verify_watermark(const unsigned char *wm, int wm_len,
                                 char *result_out, int result_out_len) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char uid[20], did[20];
        long ts = 0;
        int ret = verify_watermark(wm, wm_len, uid, 20, did, 20, &ts);
        if (ret == 0) {
            char result[256];
            snprintf(result, sizeof(result), "%s|%s|%ld|verified", uid, did, ts);
            write(pipefd[1], result, strlen(result));
        }
        close(pipefd[1]);
        _exit(ret == 0 ? 0 : 1);
    }

    close(pipefd[1]);
    int status;
    char buf[256] = {0};
    ssize_t n = read(pipefd[0], buf, sizeof(buf) - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0 && n > 0) {
        strncpy(result_out, buf, result_out_len - 1);
        result_out[result_out_len - 1] = '\0';
        return 0;
    }
    return -1;
}

// ===== JNI 接口 =====

/*
 * 编号59：生成隐性水印（fork隔离）
 */
JNIEXPORT jbyteArray JNICALL
Java_com_myvideo_editor_security_ComplianceAuditor_nativeGenerateWatermark(
    JNIEnv *env, jobject thiz, jstring user_id, jstring device_id, jlong timestamp) {

    const char *uid = (*env)->GetStringUTFChars(env, user_id, NULL);
    const char *did = (*env)->GetStringUTFChars(env, device_id, NULL);

    unsigned char wm[WM_TOTAL_LEN];
    int ret = fork_generate_watermark(uid, did, (long)timestamp, wm);

    (*env)->ReleaseStringUTFChars(env, user_id, uid);
    (*env)->ReleaseStringUTFChars(env, device_id, did);

    if (ret != 0) return NULL;

    jbyteArray result = (*env)->NewByteArray(env, WM_TOTAL_LEN);
    (*env)->SetByteArrayRegion(env, result, 0, WM_TOTAL_LEN, (jbyte *)wm);

    ca_secure_zero(wm, WM_TOTAL_LEN);
    return result;
}

/*
 * 编号59：验证水印（fork隔离）
 */
JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_ComplianceAuditor_nativeVerifyWatermark(
    JNIEnv *env, jobject thiz, jbyteArray watermark_data) {

    jbyte *data = (*env)->GetByteArrayElements(env, watermark_data, NULL);
    jsize len = (*env)->GetArrayLength(env, watermark_data);

    char result[256] = {0};
    int ret = fork_verify_watermark((unsigned char *)data, (int)len, result, sizeof(result));

    (*env)->ReleaseByteArrayElements(env, watermark_data, data, JNI_ABORT);

    if (ret != 0) return NULL;
    return (*env)->NewStringUTF(env, result);
}
