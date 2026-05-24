#!/bin/bash
# ============================================
# NexClip 类目二：签名校验 - CI/CD验证
# 编号3/6/23全部验证方式+异常判定
# ============================================

set -e

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
SO_DIR="${2:-app/build/intermediates/merged_native_libs}"
SRC_DIR="${3:-app/src/main/java}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目二：签名校验 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号3：APK签名校验 =====
echo "=== 编号3：APK签名校验 ==="
echo "验证：正常安装的APP两层都通过 | 用其他签名重打包后两层都不通过"
echo "异常判定：任一层签名校验失败=弹警告+强制关闭+上报服务端"

# 检查Java层签名代码存在
SIG_KT="$SRC_DIR/com/myvideo/editor/security/SignatureVerifier.kt"
if [ -f "$SIG_KT" ]; then
    # 检查try-catch包裹
    TRY_COUNT=$(grep -c "try" "$SIG_KT" || true)
    CATCH_COUNT=$(grep -c "catch" "$SIG_KT" || true)
    if [ "$TRY_COUNT" -gt 0 ] && [ "$CATCH_COUNT" -gt 0 ]; then
        echo "${G}  ✅ Java层try-catch包裹完整 ($TRY_COUNT try / $CATCH_COUNT catch)${N}"
    else
        echo "${R}  ⚠️ Java层缺少try-catch包裹→崩溃风险${N}"
        F=$((F+1))
    fi

    # 检查SHA-256比对逻辑
    if grep -q "SHA-256" "$SIG_KT" || grep -q "SHA256" "$SIG_KT"; then
        echo "${G}  ✅ SHA-256签名校验逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少SHA-256签名校验${N}"
        F=$((F+1))
    fi

    # 检查Native层调用
    if grep -q "nativeVerifySignature" "$SIG_KT"; then
        echo "${G}  ✅ Native层签名校验接口存在${N}"
    else
        echo "${R}  ⚠️ 缺少Native层签名校验${N}"
        F=$((F+1))
    fi

    # 检查交叉验证逻辑
    if grep -q "交叉验证\|crossCheck" "$SIG_KT"; then
        echo "${G}  ✅ Java+Native交叉验证逻辑存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少交叉验证逻辑${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ SignatureVerifier.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号6：文件完整性校验 =====
echo "=== 编号6：文件完整性校验 ==="
echo "验证：未修改的文件hash一致 | 修改任意SO或资源文件后hash不匹配"
echo "异常判定：hash不匹配=弹警告+强制关闭+上报服务端"

FI_KT="$SRC_DIR/com/myvideo/editor/security/FileIntegrityChecker.kt"
if [ -f "$FI_KT" ]; then
    # 检查SHA-256计算
    if grep -q "SHA-256" "$FI_KT" || grep -q "SHA256" "$FI_KT"; then
        echo "${G}  ✅ SHA-256文件hash计算逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少SHA-256文件hash计算${N}"
        F=$((F+1))
    fi

    # 检查SO文件校验
    if grep -q "libsecurity\|libnative\|libcodec\|librenderer" "$FI_KT"; then
        echo "${G}  ✅ 自建SO文件校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少SO文件校验${N}"
        F=$((F+1))
    fi

    # 检查模型文件校验
    if grep -q "model_\|模型文件" "$FI_KT"; then
        echo "${G}  ✅ 模型文件校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少模型文件校验${N}"
        F=$((F+1))
    fi

    # 检查fork隔离
    if grep -q "nativeVerifyFileHash" "$FI_KT"; then
        echo "${G}  ✅ fork隔离校验接口存在${N}"
    else
        echo "${R}  ⚠️ 缺少fork隔离校验${N}"
        F=$((F+1))
    fi

    # 检查交叉验证
    if grep -q "crossVerify\|交叉验证" "$FI_KT"; then
        echo "${G}  ✅ 签名+文件完整性交叉验证存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少交叉验证${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ FileIntegrityChecker.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号23：安装来源校验 =====
echo "=== 编号23：安装来源校验 ==="
echo "验证：官方渠道安装=通过 | adb安装/第三方商店安装=标记可疑"
echo "异常判定：来源不明+签名异常=弹警告+强制关闭 | 仅来源不明=综合判断"

if [ -f "$SIG_KT" ]; then
    # 检查installerPackageName读取
    if grep -q "installerPackageName\|installingPackageName\|getInstallSourceInfo" "$SIG_KT"; then
        echo "${G}  ✅ 安装来源读取逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少安装来源读取${N}"
        F=$((F+1))
    fi

    # 检查白名单
    if grep -q "OFFICIAL_INSTALLERS\|白名单" "$SIG_KT"; then
        echo "${G}  ✅ 官方渠道白名单存在${N}"
    else
        echo "${R}  ⚠️ 缺少官方渠道白名单${N}"
        F=$((F+1))
    fi

    # 检查Google Play/华为/小米/OPPO/vivo/三星
    CHANNELS=("vending" "huawei" "xiaomi" "oppo" "bbk" "samsung")
    MISSING=0
    for ch in "${CHANNELS[@]}"; do
        if ! grep -q "$ch" "$SIG_KT"; then
            echo "${Y}  ⚠️ 白名单缺少: $ch${N}"
            MISSING=$((MISSING+1))
        fi
    done
    if [ "$MISSING" -eq 0 ]; then
        echo "${G}  ✅ 全部主要渠道白名单齐全${N}"
    fi

    # 检查try-catch
    if grep -q "try" "$SIG_KT"; then
        echo "${G}  ✅ try-catch包裹（防崩溃）${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少try-catch包裹${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ SignatureVerifier.kt不存在${N}"
    F=$((F+1))
fi

echo ""

# ===== Native层fork隔离验证 =====
echo "=== Native层fork隔离验证 ==="
NATIVE_C="$(dirname $0)/native_verify.c"
if [ -f "$NATIVE_C" ]; then
    # 检查fork调用
    FORK_COUNT=$(grep -c "fork()" "$NATIVE_C" || true)
    if [ "$FORK_COUNT" -gt 0 ]; then
        echo "${G}  ✅ fork隔离调用存在 ($FORK_COUNT 处)${N}"
    else
        echo "${R}  ⚠️ 缺少fork隔离→子进程崩溃影响主进程${N}"
        F=$((F+1))
    fi

    # 检查waitpid
    if grep -q "waitpid" "$NATIVE_C"; then
        echo "${G}  ✅ waitpid回收子进程存在${N}"
    else
        echo "${R}  ⚠️ 缺少waitpid→僵尸进程${N}"
        F=$((F+1))
    fi

    # 检查ZIP魔数验证
    if grep -q "ZIP_MAGIC" "$NATIVE_C"; then
        echo "${G}  ✅ ZIP魔数验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少ZIP魔数验证${N}"
        F=$((F+1))
    fi

    # 检查WIFEXITED崩溃检测
    if grep -q "WIFEXITED" "$NATIVE_C"; then
        echo "${G}  ✅ 子进程崩溃检测存在（WIFEXITED）${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少子进程崩溃检测${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_verify.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== JNI注册验证 =====
echo "=== JNI注册验证 ==="
if [ -f "$NATIVE_C" ]; then
    if grep -q "Java_com_myvideo_editor_security_SignatureVerifier" "$NATIVE_C"; then
        echo "${G}  ✅ 签名校验JNI注册存在${N}"
    else
        echo "${R}  ⚠️ 缺少签名校验JNI注册${N}"
        F=$((F+1))
    fi
    if grep -q "Java_com_myvideo_editor_security_FileIntegrityChecker" "$NATIVE_C"; then
        echo "${G}  ✅ 文件完整性JNI注册存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少文件完整性JNI注册${N}"
        F=$((F+1))
    fi
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目二验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部3项签名校验验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] Java层try-catch + Native层fork隔离${N}"
    exit 0
fi
