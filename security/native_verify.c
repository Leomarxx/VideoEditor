/*
 * ============================================
 * NexClip 类目二：签名校验 - Native层
 * 编号3：APK签名校验（Native层fork隔离）
 * 编号6：文件完整性校验（fork隔离hash计算）
 *
 * 做什么：fork子进程执行校验，子进程崩溃不影响主进程
 * 程度：子进程崩溃不影响主进程
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
#include <errno.h>
#include <fcntl.h>

#define TAG "NativeVerify"
#define ZIP_MAGIC_0 0x50
#define ZIP_MAGIC_1 0x4B
#define ZIP_MAGIC_2 0x03
#define ZIP_MAGIC_3 0x04
#define READ_BUF_SIZE 4096

/*
 * 计算SHA-256（简化版，用于文件校验）
 * 生产环境使用OpenSSL/BoringSSL
 */
typedef struct {
    unsigned int state[8];
    unsigned long long count;
    unsigned char buffer[64];
} sha256_ctx;

static const unsigned int K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
    0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
    0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
    0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

#define ROTR(x, n) (((x) >> (n)) | ((x) << (32 - (n))))
#define CH(x, y, z) (((x) & (y)) ^ (~(x) & (z)))
#define MAJ(x, y, z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define EP0(x) (ROTR(x, 2) ^ ROTR(x, 13) ^ ROTR(x, 22))
#define EP1(x) (ROTR(x, 6) ^ ROTR(x, 11) ^ ROTR(x, 25))
#define SIG0(x) (ROTR(x, 7) ^ ROTR(x, 18) ^ ((x) >> 3))
#define SIG1(x) (ROTR(x, 17) ^ ROTR(x, 19) ^ ((x) >> 10))

static void sha256_init(sha256_ctx *ctx) {
    ctx->state[0] = 0x6a09e667; ctx->state[1] = 0xbb67ae85;
    ctx->state[2] = 0x3c6ef372; ctx->state[3] = 0xa54ff53a;
    ctx->state[4] = 0x510e527f; ctx->state[5] = 0x9b05688c;
    ctx->state[6] = 0x1f83d9ab; ctx->state[7] = 0x5be0cd19;
    ctx->count = 0;
}

static void sha256_transform(sha256_ctx *ctx, const unsigned char data[]) {
    unsigned int a, b, c, d, e, f, g, h, i, t1, t2, m[64];
    for (i = 0; i < 16; i++)
        m[i] = ((unsigned int)data[i*4]<<24)|((unsigned int)data[i*4+1]<<16)|
               ((unsigned int)data[i*4+2]<<8)|((unsigned int)data[i*4+3]);
    for (i = 16; i < 64; i++)
        m[i] = SIG1(m[i-2]) + m[i-7] + SIG0(m[i-15]) + m[i-16];
    a=ctx->state[0]; b=ctx->state[1]; c=ctx->state[2]; d=ctx->state[3];
    e=ctx->state[4]; f=ctx->state[5]; g=ctx->state[6]; h=ctx->state[7];
    for (i = 0; i < 64; i++) {
        t1 = h + EP1(e) + CH(e,f,g) + K[i] + m[i];
        t2 = EP0(a) + MAJ(a,b,c);
        h=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
    }
    ctx->state[0]+=a; ctx->state[1]+=b; ctx->state[2]+=c; ctx->state[3]+=d;
    ctx->state[4]+=e; ctx->state[5]+=f; ctx->state[6]+=g; ctx->state[7]+=h;
}

static void sha256_update(sha256_ctx *ctx, const unsigned char data[], size_t len) {
    size_t i, j = (size_t)(ctx->count & 63);
    ctx->count += len;
    for (i = 0; i < len; i++) {
        ctx->buffer[j++] = data[i];
        if (j == 64) { sha256_transform(ctx, ctx->buffer); j = 0; }
    }
}

static void sha256_final(sha256_ctx *ctx, unsigned char hash[]) {
    unsigned int i, j = (unsigned int)(ctx->count & 63);
    ctx->buffer[j++] = 0x80;
    if (j > 56) { while (j < 64) ctx->buffer[j++] = 0; sha256_transform(ctx, ctx->buffer); j = 0; }
    while (j < 56) ctx->buffer[j++] = 0;
    ctx->count *= 8;
    for (i = 0; i < 8; i++) ctx->buffer[56+i] = (unsigned char)(ctx->count >> (56-i*8));
    sha256_transform(ctx, ctx->buffer);
    for (i = 0; i < 8; i++) {
        hash[i*4]   = (unsigned char)(ctx->state[i]>>24);
        hash[i*4+1] = (unsigned char)(ctx->state[i]>>16);
        hash[i*4+2] = (unsigned char)(ctx->state[i]>>8);
        hash[i*4+3] = (unsigned char)(ctx->state[i]);
    }
}

/*
 * 将hash转为十六进制字符串
 */
static void hash_to_hex(const unsigned char hash[], char hex[], int len) {
    for (int i = 0; i < len; i++) {
        sprintf(hex + i * 2, "%02x", hash[i]);
    }
    hex[len * 2] = '\0';
}

/*
 * 在子进程中计算文件SHA-256
 * fork隔离：子进程崩溃不影响主进程
 */
static int compute_file_hash(const char *path, char *hex_out) {
    sha256_ctx ctx;
    unsigned char hash[32];
    unsigned char buf[READ_BUF_SIZE];
    FILE *f = fopen(path, "rb");
    if (!f) return -1;

    sha256_init(&ctx);
    size_t n;
    while ((n = fread(buf, 1, READ_BUF_SIZE, f)) > 0) {
        sha256_update(&ctx, buf, n);
    }
    fclose(f);
    sha256_final(&ctx, hash);
    hash_to_hex(hash, hex_out, 32);
    return 0;
}

/*
 * 子进程中验证APK文件头（编号3）
 * 检查ZIP魔数 + 计算前4KB hash
 */
static int child_verify_apk(const char *apk_path, const char *expected_hash) {
    unsigned char header[READ_BUF_SIZE];
    FILE *f = fopen(apk_path, "rb");
    if (!f) return -1;

    size_t n = fread(header, 1, READ_BUF_SIZE, f);
    fclose(f);

    if (n < 4) return -1;

    // 验证ZIP魔数
    if (header[0] != ZIP_MAGIC_0 || header[1] != ZIP_MAGIC_1 ||
        header[2] != ZIP_MAGIC_2 || header[3] != ZIP_MAGIC_3) {
        return -2;
    }

    // 计算前4KB SHA-256
    sha256_ctx ctx;
    unsigned char hash[32];
    char hex[65];
    sha256_init(&ctx);
    sha256_update(&ctx, header, n);
    sha256_final(&ctx, hash);
    hash_to_hex(hash, hex, 32);

    // 比对预存值
    if (strcasecmp(hex, expected_hash) != 0) {
        return -3;
    }

    return 0;
}

/*
 * 子进程中验证文件完整性（编号6）
 * 计算文件SHA-256和预存值比对
 */
static int child_verify_file(const char *file_path, const char *expected_hash) {
    char hex[65];
    if (compute_file_hash(file_path, hex) != 0) {
        return -1;
    }
    if (strcasecmp(hex, expected_hash) != 0) {
        return -2;
    }
    return 0;
}

/*
 * fork隔离执行验证
 * 子进程崩溃不影响主进程
 * 返回：0=通过，1=失败，-1=异常
 */
static int fork_verify(const char *path, const char *expected_hash, int mode) {
    pid_t pid = fork();

    if (pid < 0) {
        // fork失败
        return -1;
    }

    if (pid == 0) {
        // 子进程
        int result;
        if (mode == 0) {
            result = child_verify_apk(path, expected_hash);
        } else {
            result = child_verify_file(path, expected_hash);
        }
        _exit(result == 0 ? 0 : 1);
    }

    // 父进程
    int status;
    pid_t w = waitpid(pid, &status, 0);

    if (w < 0) {
        // waitpid失败
        return -1;
    }

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status) == 0 ? 0 : 1;
    }

    // 子进程崩溃（信号终止）
    // 崩溃率：零（主进程）
    return 1;
}

// ===== JNI 接口 =====

/*
 * 编号3：Native层APK签名校验
 * Java_com_myvideo_editor_security_SignatureVerifier_nativeVerifySignature
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_SignatureVerifier_nativeVerifySignature(
    JNIEnv *env, jobject thiz, jstring apk_path, jstring expected_hash) {

    const char *path = (*env)->GetStringUTFChars(env, apk_path, NULL);
    const char *hash = (*env)->GetStringUTFChars(env, expected_hash, NULL);
    jboolean result = JNI_FALSE;

    if (path && hash) {
        // fork隔离：子进程崩溃不影响主进程
        int ret = fork_verify(path, hash, 0);
        result = (ret == 0) ? JNI_TRUE : JNI_FALSE;
    }

    if (path) (*env)->ReleaseStringUTFChars(env, apk_path, path);
    if (hash) (*env)->ReleaseStringUTFChars(env, expected_hash, hash);

    return result;
}

/*
 * 编号6：Native层文件完整性校验
 * Java_com_myvideo_editor_security_FileIntegrityChecker_nativeVerifyFileHash
 */
JNIEXPORT jboolean JNICALL
Java_com_myvideo_editor_security_FileIntegrityChecker_nativeVerifyFileHash(
    JNIEnv *env, jobject thiz, jstring file_path, jstring expected_hash) {

    const char *path = (*env)->GetStringUTFChars(env, file_path, NULL);
    const char *hash = (*env)->GetStringUTFChars(env, expected_hash, NULL);
    jboolean result = JNI_FALSE;

    if (path && hash) {
        // fork隔离：子进程崩溃不影响主进程
        int ret = fork_verify(path, hash, 1);
        result = (ret == 0) ? JNI_TRUE : JNI_FALSE;
    }

    if (path) (*env)->ReleaseStringUTFChars(env, file_path, path);
    if (hash) (*env)->ReleaseStringUTFChars(env, expected_hash, hash);

    return result;
}
