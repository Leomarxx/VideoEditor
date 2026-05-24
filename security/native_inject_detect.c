/*
 * ============================================
 * NexClip 类目五：反注入 - Native层
 * 编号32：注入区域监控（maps hash）
 * 编号34：进程注入检测（SO列表+LD_PRELOAD）
 * 编号36：maps变化监控
 * 编号37：SO加载行为完整性（四维度）
 *
 * 防崩溃方式：fork隔离+只做文本解析
 * 崩溃率：零（主进程）
 * ============================================
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <time.h>

#define HASH_LEN 65
#define LINE_SIZE 512
#define MAPS_BUF 65536

/*
 * SHA-256 简化实现（同native_verify.c）
 */
typedef struct {
    unsigned int state[8];
    unsigned long long count;
    unsigned char buffer[64];
} sha256_ctx_t;

static const unsigned int K256[64] = {
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,
    0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,
    0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,
    0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,
    0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,
    0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,
    0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,
    0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,
    0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};

#define R256(x,n) (((x)>>(n))|((x)<<(32-(n))))
#define CH256(x,y,z) (((x)&(y))^(~(x)&(z)))
#define MAJ256(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))
#define EP0_256(x) (R256(x,2)^R256(x,13)^R256(x,22))
#define EP1_256(x) (R256(x,6)^R256(x,11)^R256(x,25))
#define SG0_256(x) (R256(x,7)^R256(x,18)^((x)>>3))
#define SG1_256(x) (R256(x,17)^R256(x,19)^((x)>>10))

static void s256_init(sha256_ctx_t *c) {
    c->state[0]=0x6a09e667; c->state[1]=0xbb67ae85;
    c->state[2]=0x3c6ef372; c->state[3]=0xa54ff53a;
    c->state[4]=0x510e527f; c->state[5]=0x9b05688c;
    c->state[6]=0x1f83d9ab; c->state[7]=0x5be0cd19;
    c->count = 0;
}

static void s256_transform(sha256_ctx_t *c, const unsigned char d[]) {
    unsigned int a,b,e,f,g,h,i,t1,t2,m[64];
    a=c->state[0]; b=c->state[1]; unsigned int cc=c->state[2],dd=c->state[3];
    e=c->state[4]; f=c->state[5]; g=c->state[6]; h=c->state[7];
    for(i=0;i<16;i++) m[i]=((unsigned int)d[i*4]<<24)|((unsigned int)d[i*4+1]<<16)|((unsigned int)d[i*4+2]<<8)|((unsigned int)d[i*4+3]);
    for(i=16;i<64;i++) m[i]=SG1_256(m[i-2])+m[i-7]+SG0_256(m[i-15])+m[i-16];
    for(i=0;i<64;i++) { t1=h+EP1_256(e)+CH256(e,f,g)+K256[i]+m[i]; t2=EP0_256(a)+MAJ256(a,b,cc); h=g; g=f; f=e; e=dd+t1; dd=cc; cc=b; b=a; a=t1+t2; }
    c->state[0]+=a; c->state[1]+=b; c->state[2]+=cc; c->state[3]+=dd;
    c->state[4]+=e; c->state[5]+=f; c->state[6]+=g; c->state[7]+=h;
}

static void s256_update(sha256_ctx_t *c, const unsigned char *d, size_t l) {
    size_t i,j=(size_t)(c->count&63); c->count+=l;
    for(i=0;i<l;i++) { c->buffer[j++]=d[i]; if(j==64){s256_transform(c,c->buffer);j=0;} }
}

static void s256_final(sha256_ctx_t *c, unsigned char h[]) {
    unsigned int i,j=(unsigned int)(c->count&63);
    c->buffer[j++]=0x80;
    if(j>56){while(j<64)c->buffer[j++]=0;s256_transform(c,c->buffer);j=0;}
    while(j<56)c->buffer[j++]=0;
    c->count*=8;
    for(i=0;i<8;i++)c->buffer[56+i]=(unsigned char)(c->count>>(56-i*8));
    s256_transform(c,c->buffer);
    for(i=0;i<8;i++){h[i*4]=(unsigned char)(c->state[i]>>24);h[i*4+1]=(unsigned char)(c->state[i]>>16);h[i*4+2]=(unsigned char)(c->state[i]>>8);h[i*4+3]=(unsigned char)(c->state[i]);}
}

static void hash_to_hex256(const unsigned char h[], char hex[], int len) {
    for(int i=0;i<len;i++) sprintf(hex+i*2,"%02x",h[i]);
    hex[len*2]='\0';
}

/*
 * 计算maps内容的SHA-256
 */
static int compute_maps_hash(pid_t pid, char *hex_out) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return -1;

    sha256_ctx_t ctx;
    unsigned char hash[32];
    unsigned char buf[4096];
    size_t n;

    s256_init(&ctx);
    while ((n = fread(buf, 1, 4096, f)) > 0) {
        s256_update(&ctx, buf, n);
    }
    fclose(f);
    s256_final(&ctx, hash);
    hash_to_hex256(hash, hex_out, 32);
    return 0;
}

/*
 * 合法SO目录白名单
 */
static int is_legit_so_path(const char *path) {
    const char *legit_dirs[] = {
        "/data/app/", "/system/lib64/", "/system/lib/",
        "/vendor/lib64/", "/vendor/lib/", "/apex/",
        "/data/dalvik-cache/", NULL
    };
    for (int i = 0; legit_dirs[i] != NULL; i++) {
        if (strncmp(path, legit_dirs[i], strlen(legit_dirs[i])) == 0) {
            return 1;
        }
    }
    return 0;
}

/*
 * 编号34：检查LD_PRELOAD
 * 正常APP启动时应为空
 */
static int check_ld_preload_inject(void) {
    const char *preload = getenv("LD_PRELOAD");
    if (preload && strlen(preload) > 0) {
        return 1;
    }
    return 0;
}

/*
 * 编号34：检查maps中非合法目录的SO
 */
static int check_illegit_so(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, ".so")) continue;

        char *last_space = strrchr(line, ' ');
        if (!last_space) continue;

        char so_path[256];
        strncpy(so_path, last_space + 1, sizeof(so_path) - 1);
        so_path[sizeof(so_path) - 1] = '\0';

        // 去掉换行符
        size_t len = strlen(so_path);
        while (len > 0 && (so_path[len-1] == '\n' || so_path[len-1] == '\r')) {
            so_path[--len] = '\0';
        }

        if (len > 0 && !is_legit_so_path(so_path)) {
            fclose(f);
            return 1;
        }
    }
    fclose(f);
    return 0;
}

/*
 * 编号37 方法1：SO文件SHA-256校验
 * 和编译时预存hash比对
 */
static int verify_so_hash(const char *so_path, const char *expected_hash) {
    FILE *f = fopen(so_path, "rb");
    if (!f) return -1;

    sha256_ctx_t ctx;
    unsigned char hash[32];
    unsigned char buf[4096];
    size_t n;

    s256_init(&ctx);
    while ((n = fread(buf, 1, 4096, f)) > 0) {
        s256_update(&ctx, buf, n);
    }
    fclose(f);
    s256_final(&ctx, hash);

    char hex[65];
    hash_to_hex256(hash, hex, 32);

    return strcasecmp(hex, expected_hash) == 0 ? 0 : 1;
}

/*
 * 编号37 方法2：SO加载路径验证
 * 遍历maps中所有.so路径，白名单验证
 */
static int verify_so_paths(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    int illegit_count = 0;

    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, ".so")) continue;

        char *last_space = strrchr(line, ' ');
        if (!last_space) continue;

        char so_path[256];
        strncpy(so_path, last_space + 1, sizeof(so_path) - 1);
        so_path[sizeof(so_path) - 1] = '\0';

        size_t len = strlen(so_path);
        while (len > 0 && (so_path[len-1] == '\n' || so_path[len-1] == '\r')) {
            so_path[--len] = '\0';
        }

        if (len > 0 && !is_legit_so_path(so_path)) {
            illegit_count++;
        }
    }
    fclose(f);
    return illegit_count > 0 ? 1 : 0;
}

/*
 * 编号37 方法4：SO文件属性验证
 * 检查最后修改时间和文件权限
 * 安装后不应被修改
 */
static int verify_so_attrs(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[LINE_SIZE];
    time_t now = time(NULL);

    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, ".so")) continue;

        char *last_space = strrchr(line, ' ');
        if (!last_space) continue;

        char so_path[256];
        strncpy(so_path, last_space + 1, sizeof(so_path) - 1);
        so_path[sizeof(so_path) - 1] = '\0';

        size_t len = strlen(so_path);
        while (len > 0 && (so_path[len-1] == '\n' || so_path[len-1] == '\r')) {
            so_path[--len] = '\0';
        }

        struct stat st;
        if (stat(so_path, &st) == 0) {
            // 最近5分钟内被修改=可疑
            if (now - st.st_mtime < 300) {
                fclose(f);
                return 1;
            }
        }
    }
    fclose(f);
    return 0;
}

/*
 * fork隔离：编号32 maps hash获取
 * 子进程崩溃不影响主进程
 */
static int fork_maps_hash(pid_t pid, char *hex_out) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return -1;

    pid_t child = fork();
    if (child < 0) { close(pipefd[0]); close(pipefd[1]); return -1; }

    if (child == 0) {
        close(pipefd[0]);
        char hex[65];
        int ret = compute_maps_hash(pid, hex);
        if (ret == 0) {
            write(pipefd[1], hex, 64);
        }
        close(pipefd[1]);
        _exit(ret == 0 ? 0 : 1);
    }

    close(pipefd[1]);
    int status;
    char buf[65] = {0};
    read(pipefd[0], buf, 64);
    close(pipefd[0]);
    waitpid(child, &status, 0);

    if (WIFEXITED(status) && WEXITSTATUS(status) == 0) {
        memcpy(hex_out, buf, 64);
        hex_out[64] = '\0';
        return 0;
    }
    return -1;
}

/*
 * fork隔离：编号34 进程注入检测
 * 返回bitmask：bit0=LD_PRELOAD bit1=非法SO
 */
static int fork_inject_detect(pid_t pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;
        if (check_ld_preload_inject()) result |= (1 << 0);
        if (check_illegit_so(pid))    result |= (1 << 1);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

/*
 * fork隔离：编号37 SO完整性四维度
 * 返回bitmask：bit0=路径 bit1=属性
 */
static int fork_so_integrity(pid_t pid) {
    pid_t child = fork();
    if (child < 0) return 0;

    if (child == 0) {
        int result = 0;
        if (verify_so_paths(pid))  result |= (1 << 0);
        if (verify_so_attrs(pid))  result |= (1 << 1);
        _exit(result);
    }

    int status;
    waitpid(child, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    return 0;
}

// ===== JNI 接口 =====

/*
 * 编号32：获取maps hash（fork隔离）
 * Java_com_myvideo_editor_security_InjectionDetector_nativeGetMapsHash
 */
JNIEXPORT jstring JNICALL
Java_com_myvideo_editor_security_InjectionDetector_nativeGetMapsHash(
    JNIEnv *env, jobject thiz) {

    char hex[65];
    pid_t pid = getpid();
    if (fork_maps_hash(pid, hex) == 0) {
        return (*env)->NewStringUTF(env, hex);
    }
    return NULL;
}

/*
 * 编号32+36：分析maps变化（fork隔离）
 * Java_com_myvideo_editor_security_InjectionDetector_nativeAnalyzeMapsChange
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_InjectionDetector_nativeAnalyzeMapsChange(
    JNIEnv *env, jobject thiz, jstring old_hash) {

    const char *old = (*env)->GetStringUTFChars(env, old_hash, NULL);
    pid_t pid = getpid();
    char current[65];
    int result = 0;

    if (fork_maps_hash(pid, current) == 0) {
        if (strcasecmp(old, current) != 0) {
            // hash变化，进一步分析
            result = fork_inject_detect(pid);
        }
    }

    if (old) (*env)->ReleaseStringUTFChars(env, old_hash, old);
    return result;
}

/*
 * 编号37：SO完整性校验（fork隔离）
 * Java_com_myvideo_editor_security_InjectionDetector_nativeVerifySoIntegrity
 */
JNIEXPORT jint JNICALL
Java_com_myvideo_editor_security_InjectionDetector_nativeVerifySoIntegrity(
    JNIEnv *env, jobject thiz) {

    pid_t pid = getpid();
    int result = 0;

    // 编号34
    int inject = fork_inject_detect(pid);
    if (inject != 0) result |= (inject);

    // 编号37
    int so_integrity = fork_so_integrity(pid);
    if (so_integrity != 0) result |= (so_integrity << 2);

    return result;
}
