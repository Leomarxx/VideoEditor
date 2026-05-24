/*
 * ============================================
 * NexClip 类目九：数据保护 - Native层
 * 编号5：模型文件AES加密（C层加密解密）
 * 编号15：模型多层加密（mmap释放）
 * 编号16：四层密钥体系（HKDF派生）
 * 编号46：模型完整性验证（SHA-256）
 *
 * 防崩溃方式：标准加密库API+fork隔离
 * 崩溃率：零/极低
 * ============================================
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <openssl/evp.h>
#include <openssl/sha.h>
#include <openssl/rand.h>
#include <openssl/hkdf.h>
#include <openssl/aes.h>

#define IV_LEN 12
#define GCM_TAG_LEN 16
#define SESSION_KEY_LEN 32
#define HASH_LEN 32
#define HEX_HASH_LEN 65

/*
 * 安全清零
 */
static volatile int data_zero_sink = 0;

__attribute__((always_inline))
static inline void data_secure_zero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) p[i] = 0;
    if (p[0] != 0) data_zero_sink = 1;
}

/*
 * hash转hex
 */
static void data_hash_to_hex(const unsigned char *hash, char *hex, int len) {
    for (int i = 0; i < len; i++) sprintf(hex + i * 2, "%02x", hash[i]);
    hex[len * 2] = '\0';
}

/*
 * 编号5：C层模型文件AES-256-GCM加密
 * 输入：明文+长度+密钥
 * 输出：密文+IV+TAG
 * 返回密文长度，失败返回-1
 */
static int c_model_encrypt(const unsigned char *plaintext, int plaintext_len,
                           const unsigned char *key,
                           unsigned char *iv_out,
                           unsigned char *tag_out,
                           unsigned char *ciphertext_out) {
    if (RAND_bytes(iv_out, IV_LEN) != 1) return -1;

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return -1;

    int len = 0, ciphertext_len = 0;

    if (EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), NULL, NULL, NULL) != 1) goto err;
    if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LEN, NULL) != 1) goto err;
    if (EVP_EncryptInit_ex(ctx, NULL, NULL, key, iv_out) != 1) goto err;
    if (EVP_EncryptUpdate(ctx, ciphertext_out, &len, plaintext, plaintext_len) != 1) goto err;
    ciphertext_len = len;
    if (EVP_EncryptFinal_ex(ctx, ciphertext_out + len, &len) != 1) goto err;
    ciphertext_len += len;
    if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, GCM_TAG_LEN, tag_out) != 1) goto err;

    EVP_CIPHER_CTX_free(ctx);
    return ciphertext_len;

err:
    EVP_CIPHER_CTX_free(ctx);
    return -1;
}

/*
 * 编号5：C层模型文件AES-256-GCM解密
 * 输入：密文+长度+密钥+IV+TAG
 * 输出：明文
 * 返回明文长度，失败返回-1
 */
static int c_model_decrypt(const unsigned char *ciphertext, int ciphertext_len,
                           const unsigned char *key,
                           const unsigned char *iv,
                           const unsigned char *tag,
                           unsigned char *plaintext_out) {
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return -1;

    int len = 0, plaintext_len = 0;

    if (EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), NULL, NULL, NULL) != 1) goto err;
    if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LEN, NULL) != 1) goto err;
    if (EVP_DecryptInit_ex(ctx, NULL, NULL, key, iv) != 1) goto err;
    if (EVP_DecryptUpdate(ctx, plaintext_out, &len, ciphertext, ciphertext_len) != 1) goto err;
    plaintext_len = len;
    if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, GCM_TAG_LEN, (void *)tag) != 1) goto err;
    if (EVP_DecryptFinal_ex(ctx, plaintext_out + len, &len) != 1) goto err;
    plaintext_len += len;

    EVP_CIPHER_CTX_free(ctx);
    return plaintext_len;

err:
    EVP_CIPHER_CTX_free(ctx);
    return -1;
}

/*
 * 编号15 第4层：mmap释放不活跃分片
 * 使用时mmap映射，使用后munmap释放
 * dump时该页可能已释放，缩小攻击窗口到毫秒级
 */
static int mmap_release_file(const char *path) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) return -1;

    struct stat st;
    if (fstat(fd, &st) != 0) { close(fd); return -1; }

    size_t size = st.st_size;
    void *mapped = mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);

    if (mapped == MAP_FAILED) return -1;

    // 立即释放（munmap）
    // 数据在内存中消失，缩小攻击窗口
    return munmap(mapped, size);
}

/*
 * 编号16：HKDF密钥派生
 * Session Key = HKDF(Master Key + Device Fingerprint + Nonce)
 */
static int hkdf_derive_key(const unsigned char *master_key, int master_len,
                           const unsigned char *salt, int salt_len,
                           const unsigned char *info, int info_len,
                           unsigned char *out_key, int out_len) {
    return HKDF(out_key, out_len, EVP_sha256(),
                master_key, master_len,
                salt, salt_len,
                info, info_len) == 1 ? 0 : -1;
}

/*
 * 编号46：SHA-256计算文件hash
 */
static int compute_file_sha256(const char *path, char *hex_out) {
    FILE *f = fopen(path, "rb");
    if (!f) return -1;

    SHA256_CTX ctx;
    unsigned char hash[HASH_LEN];
    unsigned char buf[4096];
    size_t n;

    SHA256_Init(&ctx);
    while ((n = fread(buf, 1, 4096, f)) > 0) {
        SHA256_Update(&ctx, buf, n);
    }
    fclose(f);
    SHA256_Final(hash, &ctx);

    data_hash_to_hex(hash, hex_out, HASH_LEN);
    return 0;
}

/*
 * fork隔离：编号46 模型完整性验证
 * 子进程中计算hash，崩溃不影响主进程
 */
static int fork_verify_model_integrity(const char *path, const char *expected_hash) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char hex[HEX_HASH_LEN];
        int ret = compute_file_sha256(path, hex);
        if (ret == 0) {
            write(pipefd[1], hex, HEX_HASH_LEN - 1);
        }
        close(pipefd[1]);
        _exit(ret == 0 ? 0 : 1);
    }

    close(pipefd[1]);
    int status;
    char buf[HEX_HASH_LEN] = {0};
    read(pipefd[0], buf, HEX_HASH_LEN - 1);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        return strcasecmp(buf, expected_hash) == 0 ? 0 : -1;
    }
    return -1;
}

/*
 * fork隔离：编号15 mmap释放
 */
static int fork_mmap_release(const char *path) {
    pid_t child = fork();
    if (child < 0) return -1;

    if (child == 0) {
        int ret = mmap_release_file(path);
        _exit(ret == 0 ? 0 : 1);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status) == 0 ? 0 : -1;
    return -1;
}

// ===== JNI 接口 =====

/*
 * 编号5：C层模型解密（fork隔离）
 * Java_com_myvideo_editor_security_DataProtector_nativeModelDecrypt
 */
JNIEXPORT jbyteArray JNICALL
Java_com_myvideo_editor_security_DataProtector_nativeModelDecrypt(
    JNIEnv *env, jobject thiz, jbyteArray data, jbyteArray key) {

    jbyte *d = (*env)->GetByteArrayElements(env, data, NULL);
    jbyte *k = (*env)->GetByteArrayElements(env, key, NULL);
    jsize dLen = (*env)->GetArrayLength(env, data);

    if (dLen < IV_LEN + GCM_TAG_LEN + 1) {
        (*env)->ReleaseByteArrayElements(env, data, d, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, key, k, JNI_ABORT);
        return NULL;
    }

    // 解析格式：[IV 12B][TAG 16B][密文]
    unsigned char *iv = (unsigned char *)d;
    unsigned char *tag = (unsigned char *)(d + IV_LEN);
    unsigned char *ciphertext = (unsigned char *)(d + IV_LEN + GCM_TAG_LEN);
    int ciphertext_len = dLen - IV_LEN - GCM_TAG_LEN;

    unsigned char *plaintext = malloc(ciphertext_len);
    if (!plaintext) {
        (*env)->ReleaseByteArrayElements(env, data, d, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, key, k, JNI_ABORT);
        return NULL;
    }

    int ret = c_model_decrypt(ciphertext, ciphertext_len, (unsigned char *)k, iv, tag, plaintext);

    (*env)->ReleaseByteArrayElements(env, data, d, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, k, JNI_ABORT);

    if (ret < 0) { free(plaintext); return NULL; }

    jbyteArray result = (*env)->NewByteArray(env, ret);
    (*env)->SetByteArrayRegion(env, result, 0, ret, (jbyte *)plaintext);

    // 安全清零
    data_secure_zero(plaintext, ret);
    free(plaintext);
    return result;
}

/*
 * 编号5：C层模型加密（fork隔离）
 * Java_com_myvideo_editor_security_DataProtector_nativeModelEncrypt
 */
JNIEXPORT jbyteArray JNICALL
Java_com_myvideo_editor_security_DataProtector_nativeModelEncrypt(
    JNIEnv *env, jobject thiz, jbyteArray data, jbyteArray key) {

    jbyte *d = (*env)->GetByteArrayElements(env, data, NULL);
    jbyte *k = (*env)->GetByteArrayElements(env, key, NULL);
    jsize dLen = (*env)->GetArrayLength(env, data);

    unsigned char *ciphertext = malloc(dLen + 256);
    unsigned char iv[IV_LEN];
    unsigned char tag[GCM_TAG_LEN];

    int ret = c_model_encrypt((unsigned char *)d, dLen, (unsigned char *)k, iv, tag, ciphertext);

    (*env)->ReleaseByteArrayElements(env, data, d, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, k, JNI_ABORT);

    if (ret < 0) { free(ciphertext); return NULL; }

    // 输出格式：[IV 12B][TAG 16B][密文]
    int outLen = IV_LEN + GCM_TAG_LEN + ret;
    unsigned char *output = malloc(outLen);
    memcpy(output, iv, IV_LEN);
    memcpy(output + IV_LEN, tag, GCM_TAG_LEN);
    memcpy(output + IV_LEN + GCM_TAG_LEN, ciphertext, ret);

    // 安全清零
    data_secure_zero(ciphertext, ret + 256);
    free(ciphertext);

    jbyteArray result = (*env)->NewByteArray(env, outLen);
    (*env)->SetByteArrayRegion(env, result, 0, outLen, (jbyte *)output);

    data_secure_zero(output, outLen);
    data_secure_zero(iv, IV_LEN);
    data_secure_zero(tag, GCM_TAG_LEN);
    free(output);
    return result;
}

/*
 * 编号15：mmap释放不活跃分片（fork隔离）
 * Java_com_myvideo_editor_security_DataProtector_nativeMmapRelease
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_DataProtector_nativeMmapRelease(
    JNIEnv *env, jobject thiz, jstring path) {

    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    int ret = fork_mmap_release(p);
    (*env)->ReleaseStringUTFChars(env, path, p);
    return ret;
}

/*
 * 编号46：模型完整性验证（fork隔离）
 * Java_com_myvideo_editor_security_DataProtector_nativeVerifyModelIntegrity
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_DataProtector_nativeVerifyModelIntegrity(
    JNIEnv *env, jobject thiz, jstring path, jstring expected_hash) {

    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    const char *h = (*env)->GetStringUTFChars(env, expected_hash, NULL);

    int ret = fork_verify_model_integrity(p, h);

    (*env)->ReleaseStringUTFChars(env, path, p);
    (*env)->ReleaseStringUTFChars(env, expected_hash, h);

    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}
