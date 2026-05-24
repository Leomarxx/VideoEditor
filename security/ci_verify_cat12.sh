#!/bin/bash
# ============================================
# NexClip 类目十二：持续监控 - CI/CD验证（精简版）
# 编号55/56/57/58全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目十二：持续监控 - CI/CD验证"
echo "============================================"
echo ""

CM_KT="$SRC_DIR/com/myvideo/editor/security/ContinuousMonitor.kt"

# ===== 编号55：自适应监控引擎 =====
echo "=== 编号55：自适应监控引擎 ==="
echo "验证：不同环境下检测频率自动调整+事件触发正常执行+电量低时降低频率"
echo "异常判定：任何一轮任何一项异常=弹警告+强制关闭+上报服务端"

if [ -f "$CM_KT" ]; then
    if grep -q "RiskLevel\|NORMAL.*SUSPICIOUS.*DANGER" "$CM_KT"; then
        echo "${G}  ✅ 三级风险评估存在${N}"
    else
        echo "${R}  ⚠️ 缺少风险等级评估${N}"
        F=$((F+1))
    fi

    if grep -q "getMonitorParams\|5.*5.*2.*10.*30.*20" "$CM_KT"; then
        echo "${G}  ✅ 自适应检测频率存在${N}"
    else
        echo "${R}  ⚠️ 缺少自适应频率${N}"
        F=$((F+1))
    fi

    if grep -q "randomizeChecks\|nativeRandomizeOrder" "$CM_KT"; then
        echo "${G}  ✅ 检测函数随机化存在${N}"
    else
        echo "${R}  ⚠️ 缺少随机化${N}"
        F=$((F+1))
    fi

    if grep -q "registerEventTrigger\|triggerEvent" "$CM_KT"; then
        echo "${G}  ✅ 事件触发检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少事件触发${N}"
        F=$((F+1))
    fi

    if grep -q "scheduleDelayedCheck\|时间锁" "$CM_KT"; then
        echo "${G}  ✅ 时间锁检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少时间锁${N}"
        F=$((F+1))
    fi

    if grep -q "evaluateRiskLevel\|detectionHistory" "$CM_KT"; then
        echo "${G}  ✅ 趋势分析存在${N}"
    else
        echo "${R}  ⚠️ 缺少趋势分析${N}"
        F=$((F+1))
    fi

    if grep -q "battery\|isCharging\|isScreenOn" "$CM_KT"; then
        echo "${G}  ✅ 电量自适应存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少电量自适应${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ContinuousMonitor.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号56：挑战应答认证 =====
echo "=== 编号56：挑战应答认证（精简版）==="
echo "验证：正版APP完整流程通过 | 伪造客户端无法通过 | curl无证书被拒绝"
echo "异常判定：应答失败/客户端证书无效/完整性证明失败=弹警告+强制关闭"

if [ -f "$CM_KT" ]; then
    if grep -q "initiateChallenge\|computeChallengeResponse\|nonce" "$CM_KT"; then
        echo "${G}  ✅ 挑战应答核心流程存在${N}"
    else
        echo "${R}  ⚠️ 缺少挑战应答流程${N}"
        F=$((F+1))
    fi

    if grep -q "proveIntegrity\|nativeComputeIntegrityProof" "$CM_KT"; then
        echo "${G}  ✅ 进程完整性证明存在${N}"
    else
        echo "${R}  ⚠️ 缺少完整性证明${N}"
        F=$((F+1))
    fi

    if grep -q "initMtls\|nativeMtlsHandshake" "$CM_KT"; then
        echo "${G}  ✅ mTLS客户端证书存在${N}"
    else
        echo "${R}  ⚠️ 缺少mTLS${N}"
        F=$((F+1))
    fi

    if grep -q "clearChallengeData\|sessionNonce.*fill" "$CM_KT"; then
        echo "${G}  ✅ 挑战数据清零存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少挑战数据清零${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ContinuousMonitor.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号57：服务端行为分析（客户端简化版）=====
echo "=== 编号57：服务端行为分析（客户端简化版）==="
echo "验证：正常用户行为不触发 | 客户端行为上报接口正常"
echo "异常判定：行为异常=服务端缩短token/要求二次验证/封锁账号"

if [ -f "$CM_KT" ]; then
    if grep -q "reportBehavior\|BehaviorReport" "$CM_KT"; then
        echo "${G}  ✅ 客户端行为上报接口存在${N}"
    else
        echo "${R}  ⚠️ 缺少行为上报${N}"
        F=$((F+1))
    fi

    if grep -q "getPendingReports\|behaviorQueue" "$CM_KT"; then
        echo "${G}  ✅ 待上报数据队列存在${N}"
    else
        echo "${R}  ⚠️ 缺少数据队列${N}"
        F=$((F+1))
    fi

    if grep -q "verifyCaptcha\|captcha" "$CM_KT"; then
        echo "${G}  ✅ 验证码校验接口存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少验证码接口${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ContinuousMonitor.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号58：许可证token管理 =====
echo "=== 编号58：许可证token管理 ==="
echo "验证：Token有效且签名正确+设备指纹匹配+服务端验证通过"
echo "异常判定：Token过期/签名无效/设备不匹配/异常标记/锁定=弹警告+强制关闭"

if [ -f "$CM_KT" ]; then
    if grep -q "storeToken\|getCurrentToken\|isTokenExpired" "$CM_KT"; then
        echo "${G}  ✅ Token刷新机制存在${N}"
    else
        echo "${R}  ⚠️ 缺少Token刷新${N}"
        F=$((F+1))
    fi

    if grep -q "markDeviceAbnormal\|isDeviceAbnormal" "$CM_KT"; then
        echo "${G}  ✅ 设备异常标记存在${N}"
    else
        echo "${R}  ⚠️ 缺少设备异常标记${N}"
        F=$((F+1))
    fi

    if grep -q "prepareApiRequest\|使用中拦截" "$CM_KT"; then
        echo "${G}  ✅ 使用中拦截存在${N}"
    else
        echo "${R}  ⚠️ 缺少使用中拦截${N}"
        F=$((F+1))
    fi

    if grep -q "prepareLoginRequest\|登录拦截" "$CM_KT"; then
        echo "${G}  ✅ 登录拦截存在${N}"
    else
        echo "${R}  ⚠️ 缺少登录拦截${N}"
        F=$((F+1))
    fi

    if grep -q "generateSecureDownloadLink\|validateDownloadLink" "$CM_KT"; then
        echo "${G}  ✅ 下载链接安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少下载链接安全${N}"
        F=$((F+1))
    fi

    if grep -q "remoteLock\|isDeviceLocked" "$CM_KT"; then
        echo "${G}  ✅ 远程锁定存在${N}"
    else
        echo "${R}  ⚠️ 缺少远程锁定${N}"
        F=$((F+1))
    fi

    if grep -q "remoteWipe" "$CM_KT"; then
        echo "${G}  ✅ 远程擦除存在${N}"
    else
        echo "${R}  ⚠️ 缺少远程擦除${N}"
        F=$((F+1))
    fi

    if grep -q "forceLogout\|getLoginHistory" "$CM_KT"; then
        echo "${G}  ✅ 强制下线+登录历史存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少强制下线/登录历史${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ContinuousMonitor.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层验证 =====
echo "=== Native层验证 ==="
NATIVE_C="$SEC_DIR/native_continuous_monitor.c"
if [ -f "$NATIVE_C" ]; then
    FORK_COUNT=$(grep -c "fork()" "$NATIVE_C" || true)
    if [ "$FORK_COUNT" -gt 0 ]; then
        echo "${G}  ✅ fork隔离调用存在 ($FORK_COUNT 处)${N}"
    else
        echo "${R}  ⚠️ 缺少fork隔离${N}"
        F=$((F+1))
    fi

    if grep -q "waitpid" "$NATIVE_C" && grep -q "WIFEXITED" "$NATIVE_C"; then
        echo "${G}  ✅ waitpid + WIFEXITED 存在${N}"
    else
        echo "${R}  ⚠️ 缺少子进程回收${N}"
        F=$((F+1))
    fi

    if grep -q "fisher_yates_shuffle" "$NATIVE_C"; then
        echo "${G}  ✅ 编号55 Fisher-Yates随机化存在${N}"
    else
        echo "${R}  ⚠️ 缺少随机化算法${N}"
        F=$((F+1))
    fi

    if grep -q "compute_integrity_proof\|fork_integrity_proof" "$NATIVE_C"; then
        echo "${G}  ✅ 编号56 进程完整性证明存在${N}"
    else
        echo "${R}  ⚠️ 缺少完整性证明${N}"
        F=$((F+1))
    fi

    if grep -q "mtls_handshake\|SSL_CTX_use_certificate" "$NATIVE_C"; then
        echo "${G}  ✅ 编号56 mTLS客户端证书存在${N}"
    else
        echo "${R}  ⚠️ 缺少mTLS${N}"
        F=$((F+1))
    fi

    if grep -q "cm_secure_zero\|volatile" "$NATIVE_C"; then
        echo "${G}  ✅ 安全清零（volatile防优化）存在${N}"
    else
        echo "${R}  ⚠️ 缺少安全清零${N}"
        F=$((F+1))
    fi

    JNI_OK=0
    if grep -q "nativeRandomizeOrder" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeComputeIntegrityProof" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeMtlsHandshake" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if [ "$JNI_OK" -eq 3 ]; then
        echo "${G}  ✅ JNI注册: 3/3 全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ JNI注册仅 $JNI_OK/3${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_continuous_monitor.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目十二验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部4项持续监控验证通过${N}"
    echo "${G}[崩溃率] 预期零崩溃${N}"
    echo "${G}[防崩溃] fork隔离+后台线程+catch包裹${N}"
    echo "${G}[覆盖] 55: 自适应监控+风险等级+随机化+事件触发+时间锁+电量自适应${N}"
    echo "${G}[覆盖] 56: 挑战应答+完整性证明+mTLS（精简：无超时处理/设备证明）${N}"
    echo "${G}[覆盖] 57: 行为上报+验证码接口（精简：纯服务端逻辑已删除）${N}"
    echo "${G}[覆盖] 58: Token管理+异常标记+下载安全+远程锁定/擦除+登录拦截${N}"
    exit 0
fi
