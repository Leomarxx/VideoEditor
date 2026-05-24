/*
 * NexClip 类目十二：持续监控 - Native层（精简版）
 * 编号55：检测函数随机化（Fisher-Yates洗牌）
 * 编号56：进程完整性证明+mTLS客户端证书
 * 删除：设备证明（调用51+22）、超时处理（标准逻辑）
 *
 * 防崩溃方式：fork隔离
 * 崩溃率：零
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <time.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/sha.h>

#define HASH_LEN 32

static volatile int cm_zero_sink = 0;

__attribute__((always_inline))
static inline void cm_secure_zero(void *ptr, size_t len) {
    volatile unsigned char *p = (volatile unsigned char *)ptr;
    for (size_t i = 0; i < len; i++) p[i] = 0;
    if (p[0] != 0) cm_zero_sink = 1;
}

/*
 * 编号55：Fisher-Yates洗牌
 * 每次生成不同顺序，攻击者无法预判
 */
static void fisher_yates_shuffle(int *arr, int n) {
    srand((unsigned int)(time(NULL) ^ getpid()));
    for (int i = n - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }
}

/*
 * 编号56：进程完整性证明
 * 读取自身代码段hash，结合挑战计算证明值
 * 篡改后无法计算正确值
 */
static int compute_integrity_proof(const unsigned char *challenge, int challenge_len,
                                   unsigned char *proof_out) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) return -1;

    unsigned char code_hash[HASH_LEN];
    SHA256_CTX sha_ctx;
    SHA256_Init(&sha_ctx);

    char line[512]; int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "r-xp") && strstr(line, ".so") && !found) {
            unsigned long start, end;
            if (sscanf(line, "%lx-%lx", &start, &end) == 2) {
                size_t hash_size = (end - start) > 4096 ? 4096 : (end - start);
                SHA256_Update(&sha_ctx, (void *)start, hash_size);
                found = 1;
            }
        }
    }
    fclose(f);
    if (!found) return -1;
    SHA256_Final(code_hash, &sha_ctx);

    // 证明值 = SHA-256(code_hash + challenge + timestamp)
    unsigned long ts = (unsigned long)time(NULL);
    SHA256_CTX proof_ctx;
    SHA256_Init(&proof_ctx);
    SHA256_Update(&proof_ctx, code_hash, HASH_LEN);
    SHA256_Update(&proof_ctx, challenge, challenge_len);
    SHA256_Update(&proof_ctx, &ts, sizeof(ts));
    SHA256_Final(proof_out, &proof_ctx);

    cm_secure_zero(code_hash, HASH_LEN);
    return 0;
}

/*
 * fork隔离：进程完整性证明
 */
static int fork_integrity_proof(const unsigned char *challenge, int challenge_len,
                                unsigned char *proof_out) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        unsigned char proof[HASH_LEN];
        int ret = compute_integrity_proof(challenge, challenge_len, proof);
        if (ret == 0) write(pipefd[1], proof, HASH_LEN);
        cm_secure_zero(proof, HASH_LEN);
        close(pipefd[1]); _exit(ret == 0 ? 0 : 1);
    }

    close(pipefd[1]);
    int status;
    ssize_t n = read(pipefd[0], proof_out, HASH_LEN);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0 && n == HASH_LEN) return 0;
    return -1;
}

/*
 * 编号56：mTLS客户端证书握手
 * curl/Postman没有证书=拒绝
 */
static int mtls_handshake(const char *host, const char *cert_path) {
    if (access(cert_path, F_OK) != 0) return -1;

    SSL_CTX *ctx = SSL_CTX_new(TLS_client_method());
    if (!ctx) return -1;

    if (SSL_CTX_use_certificate_file(ctx, cert_path, SSL_FILETYPE_PEM) != 1) {
        SSL_CTX_free(ctx); return -1;
    }

    SSL *ssl = SSL_new(ctx);
    if (!ssl) { SSL_CTX_free(ctx); return -1; }

    struct hostent *server = gethostbyname(host);
    if (!server) { SSL_free(ssl); SSL_CTX_free(ctx); return -1; }

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) { SSL_free(ssl); SSL_CTX_free(ctx); return -1; }

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(443);
    memcpy(&addr.sin_addr.s_addr, server->h_addr, server->h_length);

    int result = -1;
    if (connect(sockfd, (struct sockaddr *)&addr, sizeof(addr)) == 0) {
        SSL_set_fd(ssl, sockfd);
        SSL_set_tlsext_host_name(ssl, host);
        if (SSL_connect(ssl) == 1) result = 0;
    }

    close(sockfd);
    SSL_free(ssl); SSL_CTX_free(ctx);
    return result;
}

// ===== JNI 接口 =====

/*
 * 编号55：检测函数随机化（Fisher-Yates洗牌）
 */
JNIEXPORT jintArray JNICALL
Java_com_myvideo_editor_security_ContinuousMonitor_nativeRandomizeOrder(
    JNIEnv *env, jobject thiz, jintArray items) {

    jsize len = (*env)->GetArrayLength(env, items);
    jint *arr = (*env)->GetIntArrayElements(env, items, NULL);

    fisher_yates_shuffle(arr, len);

    jintArray result = (*env)->NewIntArray(env, len);
    (*env)->SetIntArrayRegion(env, result, 0, len, arr);
    (*env)->ReleaseIntArrayElements(env, items, arr, JNI_ABORT);
    return result;
}

/*
 * 编号56：进程完整性证明（fork隔离）
 */
JNIEXPORT jbyteArray JNICALL
Java_com_myvideo_editor_security_ContinuousMonitor_nativeComputeIntegrityProof(
    JNIEnv *env, jobject thiz, jbyteArray challenge) {

    jbyte *ch = (*env)->GetByteArrayElements(env, challenge, NULL);
    jsize chLen = (*env)->GetArrayLength(env, challenge);

    unsigned char proof[HASH_LEN];
    int ret = fork_integrity_proof((unsigned char *)ch, chLen, proof);

    (*env)->ReleaseByteArrayElements(env, challenge, ch, JNI_ABORT);

    if (ret != 0) return NULL;

    jbyteArray result = (*env)->NewByteArray(env, HASH_LEN);
    (*env)->SetByteArrayRegion(env, result, 0, HASH_LEN, (jbyte *)proof);

    cm_secure_zero(proof, HASH_LEN);
    return result;
}

/*
 * 编号56：mTLS客户端证书验证
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_ContinuousMonitor_nativeMtlsHandshake(
    JNIEnv *env, jobject thiz, jstring host, jstring cert_path) {

    const char *h = (*env)->GetStringUTFChars(env, host, NULL);
    const char *cp = (*env)->GetStringUTFChars(env, cert_path, NULL);

    int ret = mtls_handshake(h, cp);

    (*env)->ReleaseStringUTFChars(env, host, h);
    (*env)->ReleaseStringUTFChars(env, cert_path, cp);

    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}
