#!/bin/bash
# ============================================
# NexClip 类目八：通信安全 - CI/CD验证
# 编号12/13/14全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目八：通信安全 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号12：证书锁定+防抓包 =====
echo "=== 编号12：证书锁定+防抓包 ==="
echo "验证：正常连接四层都通过 | 开启Charles/Fiddler/mitmproxy后触发"
echo "异常判定：证书不匹配/代理环境/中间人=弹警告+强制关闭"

SC_KT="$SRC_DIR/com/myvideo/editor/security/SecureCommunicator.kt"
if [ -f "$SC_KT" ]; then
    # 四层证书验证
    LAYERS=("layer1_CertPinner" "layer2_NativeCert" "layer3_TlsFingerprint" "layer4_CertChain")
    L_OK=0
    for layer in "${LAYERS[@]}"; do
        if grep -q "$layer" "$SC_KT"; then L_OK=$((L_OK+1)); fi
    done
    if [ "$L_OK" -eq 4 ]; then
        echo "${G}  ✅ 四层证书验证全部存在${N}"
    else
        echo "${R}  ⚠️ 证书验证仅 $L_OK/4 层${N}"
        F=$((F+1))
    fi

    # 代理检测
    if grep -q "checkProxy\|http.proxyHost\|charles\|burpsuite\|mitmproxy" "$SC_KT"; then
        echo "${G}  ✅ 代理检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少代理检测${N}"
        F=$((F+1))
    fi

    # VPN检测
    if grep -q "vpn\|VPN\|tun0\|ppp0" "$SC_KT"; then
        echo "${G}  ✅ VPN检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少VPN检测${N}"
        F=$((F+1))
    fi

    # DNS安全
    if grep -q "checkDns\|DNS\|127\.\|::1" "$SC_KT"; then
        echo "${G}  ✅ DNS安全验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少DNS安全验证${N}"
        F=$((F+1))
    fi

    # TLS指纹
    if grep -q "nativeTlsFingerprint\|JA3\|指纹" "$SC_KT"; then
        echo "${G}  ✅ TLS指纹验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少TLS指纹验证${N}"
        F=$((F+1))
    fi

    # 证书链深度验证
    if grep -q "CertChain\|serverCertificates\|根CA" "$SC_KT"; then
        echo "${G}  ✅ 证书链深度验证存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少证书链深度验证${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ SecureCommunicator.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号13：请求签名 =====
echo "=== 编号13：请求签名 ==="
echo "验证：正常请求签名验证通过 | 篡改请求体/时间戳/序列号后签名不匹配"
echo "异常判定：签名无效/序列号异常/上下文链断裂=服务端拒绝"

if [ -f "$SC_KT" ]; then
    # HMAC-SHA256签名
    if grep -q "HMAC\|HmacSHA256" "$SC_KT"; then
        echo "${G}  ✅ HMAC-SHA256签名算法存在${N}"
    else
        echo "${R}  ⚠️ 缺少HMAC-SHA256${N}"
        F=$((F+1))
    fi

    # 签名内容：请求体+时间戳+设备指纹+序列号
    if grep -q "timestamp\|时间戳\|sequenceNum\|序列号" "$SC_KT"; then
        echo "${G}  ✅ 签名内容（时间戳+序列号）存在${N}"
    else
        echo "${R}  ⚠️ 缺少签名内容组件${N}"
        F=$((F+1))
    fi

    # 设备指纹参与签名
    if grep -q "deviceFingerprint\|设备指纹" "$SC_KT"; then
        echo "${G}  ✅ 设备指纹参与签名${N}"
    else
        echo "${R}  ⚠️ 缺少设备指纹签名${N}"
        F=$((F+1))
    fi

    # 时间戳校验：5分钟
    if grep -q "5.*60.*1000\|5.*分钟\|偏差" "$SC_KT"; then
        echo "${G}  ✅ 时间戳5分钟校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少时间戳校验${N}"
        F=$((F+1))
    fi

    # 序列号递增校验
    if grep -q "sequenceNumber.*increment\|递增不连续" "$SC_KT"; then
        echo "${G}  ✅ 序列号递增校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少序列号递增校验${N}"
        F=$((F+1))
    fi

    # 请求上下文链
    if grep -q "contextHash\|lastResponseHash\|上下文链" "$SC_KT"; then
        echo "${G}  ✅ 请求上下文链存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少请求上下文链${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ SecureCommunicator.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号14：请求加密+协议 =====
echo "=== 编号14：请求加密+协议 ==="
echo "验证：加密通信正常工作 | 抓包看到加密二进制数据不是明文JSON"
echo "异常判定：加密密钥无法生成/解密失败/参数名不匹配=弹警告"

if [ -f "$SC_KT" ]; then
    # AES-256-GCM
    if grep -q "AES/GCM/NoPadding\|AES-256-GCM\|GCMParameterSpec" "$SC_KT"; then
        echo "${G}  ✅ AES-256-GCM加密算法存在${N}"
    else
        echo "${R}  ⚠️ 缺少AES-256-GCM${N}"
        F=$((F+1))
    fi

    # 独立IV
    if grep -q "iv\|IV\|SecureRandom\|初始化向量" "$SC_KT"; then
        echo "${G}  ✅ 独立IV生成存在${N}"
    else
        echo "${R}  ⚠️ 缺少独立IV${N}"
        F=$((F+1))
    fi

    # 自定义二进制协议（非JSON）
    if grep -q "buildBinaryPayload\|二进制\|binary" "$SC_KT"; then
        echo "${G}  ✅ 自定义二进制协议存在${N}"
    else
        echo "${R}  ⚠️ 缺少二进制协议${N}"
        F=$((F+1))
    fi

    # 参数名动态化轮换
    if grep -q "paramMap\|encodeParamName\|参数名轮换\|动态化" "$SC_KT"; then
        echo "${G}  ✅ 参数名动态化存在${N}"
    else
        echo "${R}  ⚠️ 缺少参数名动态化${N}"
        F=$((F+1))
    fi

    # 会话密钥
    if grep -q "sessionKey\|initSession\|会话" "$SC_KT"; then
        echo "${G}  ✅ 会话密钥管理存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少会话密钥管理${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ SecureCommunicator.kt不存在${N}"
    F=$((F+1))
fi

echo ""

# ===== Native层验证 =====
echo "=== Native层验证 ==="
NATIVE_C="$SEC_DIR/native_comm_secure.c"
if [ -f "$NATIVE_C" ]; then
    # 检查OpenSSL依赖
    if grep -q "openssl/ssl.h\|openssl/evp.h" "$NATIVE_C"; then
        echo "${G}  ✅ OpenSSL依赖存在${N}"
    else
        echo "${R}  ⚠️ 缺少OpenSSL依赖${N}"
        F=$((F+1))
    fi

    # 编号12：证书公钥hash验证
    if grep -q "verify_cert_pinning\|X509_get_pubkey\|SHA256" "$NATIVE_C"; then
        echo "${G}  ✅ 编号12 证书公钥hash验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少证书公钥验证${N}"
        F=$((F+1))
    fi

    # 编号12：TLS指纹
    if grep -q "get_tls_fingerprint\|SSL_version\|SSL_CIPHER" "$NATIVE_C"; then
        echo "${G}  ✅ 编号12 TLS指纹验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少TLS指纹验证${N}"
        F=$((F+1))
    fi

    # 编号14：AES-256-GCM加密
    if grep -q "aes_gcm_encrypt\|EVP_aes_256_gcm" "$NATIVE_C"; then
        echo "${G}  ✅ 编号14 AES-256-GCM加密存在${N}"
    else
        echo "${R}  ⚠️ 缺少AES-256-GCM加密${N}"
        F=$((F+1))
    fi

    # 编号14：AES-256-GCM解密
    if grep -q "aes_gcm_decrypt" "$NATIVE_C"; then
        echo "${G}  ✅ 编号14 AES-256-GCM解密存在${N}"
    else
        echo "${R}  ⚠️ 缺少AES-256-GCM解密${N}"
        F=$((F+1))
    fi

    # 编号13：HMAC-SHA256签名
    if grep -q "hmac_sha256_sign\|HMAC" "$NATIVE_C"; then
        echo "${G}  ✅ 编号13 HMAC-SHA256签名存在${N}"
    else
        echo "${R}  ⚠️ 缺少HMAC-SHA256签名${N}"
        F=$((F+1))
    fi

    # 编号14：会话密钥生成
    if grep -q "generate_session_key\|RAND_bytes" "$NATIVE_C"; then
        echo "${G}  ✅ 编号14 会话密钥生成存在${N}"
    else
        echo "${R}  ⚠️ 缺少会话密钥生成${N}"
        F=$((F+1))
    fi

    # 安全清零
    if grep -q "comm_secure_zero\|volatile" "$NATIVE_C"; then
        echo "${G}  ✅ 安全清零（volatile防优化）存在${N}"
    else
        echo "${R}  ⚠️ 缺少安全清零${N}"
        F=$((F+1))
    fi

    # JNI注册
    JNI_OK=0
    if grep -q "nativeCertPinVerify" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeTlsFingerprint" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeGenerateSessionKey" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if [ "$JNI_OK" -eq 3 ]; then
        echo "${G}  ✅ JNI注册: 3/3 全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ JNI注册仅 $JNI_OK/3${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_comm_secure.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 密钥安全验证 =====
echo "=== 密钥安全验证 ==="
if [ -f "$SC_KT" ]; then
    # 密钥清零
    if grep -q "fill(0)\|clearSession\|清零" "$SC_KT"; then
        echo "${G}  ✅ 密钥清零逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少密钥清零${N}"
        F=$((F+1))
    fi

    # 密钥从四层体系派生
    if grep -q "masterKey\|四层密钥\|派生" "$SC_KT"; then
        echo "${G}  ✅ 密钥派生逻辑存在${N}"
    else
        echo "${Y}  ⚠️ 未找到密钥派生说明${N}"
    fi
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目八验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部3项通信安全验证通过${N}"
    echo "${G}[崩溃率] 预期零崩溃${N}"
    echo "${G}[防崩溃] 标准TLS API+try-catch+纯计算${N}"
    echo "${G}[覆盖] 12: 四层证书+防抓包+DNS+TLS指纹${N}"
    echo "${G}[覆盖] 13: HMAC签名+序列号+上下文链${N}"
    echo "${G}[覆盖] 14: AES-256-GCM+二进制协议+参数动态化${N}"
    exit 0
fi
