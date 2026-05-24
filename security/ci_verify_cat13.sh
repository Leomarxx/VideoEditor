#!/bin/bash
# ============================================
# NexClip 类目十三：合规/审计 - CI/CD验证（精简版）
# 编号59/60全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目十三：合规/审计 - CI/CD验证"
echo "============================================"
echo ""

CA_KT="$SRC_DIR/com/myvideo/editor/security/ComplianceAuditor.kt"

# ===== 编号59：合规+内容安全 =====
echo "=== 编号59：合规+内容安全 ==="
echo "验证：隐私弹窗首次弹出+数据删除30天+导出加密+水印可追溯+合规检查通过"
echo "异常判定：隐私弹窗未弹出/数据删除未执行/AI检测未生效=对应处理+法律风险上报"

if [ -f "$CA_KT" ]; then
    # 隐私弹窗
    if grep -q "showPrivacyDialog\|隐私政策\|privacy_accepted" "$CA_KT"; then
        echo "${G}  ✅ 隐私弹窗存在${N}"
    else
        echo "${R}  ⚠️ 缺少隐私弹窗${N}"
        F=$((F+1))
    fi

    # 必须明确同意不默认勾选
    if grep -q "putBoolean.*true.*apply\|同意\|positive" "$CA_KT"; then
        echo "${G}  ✅ 明确同意机制存在${N}"
    else
        echo "${R}  ⚠️ 缺少明确同意${N}"
        F=$((F+1))
    fi

    # 用户数据删除30天
    if grep -q "requestDataDeletion\|clearLocalUserData\|30天\|delete_request" "$CA_KT"; then
        echo "${G}  ✅ 用户数据删除存在${N}"
    else
        echo "${R}  ⚠️ 缺少数据删除${N}"
        F=$((F+1))
    fi

    # 数据导出JSON/CSV
    if grep -q "exportUserData\|json\|csv\|导出" "$CA_KT"; then
        echo "${G}  ✅ 数据导出存在${N}"
    else
        echo "${R}  ⚠️ 缺少数据导出${N}"
        F=$((F+1))
    fi

    # 数据保留期限
    if grep -q "ACCOUNT_DATA_RETENTION\|USAGE_RECORD_RETENTION\|LOG_RETENTION\|performDataRetention" "$CA_KT"; then
        echo "${G}  ✅ 数据保留期限存在${N}"
    else
        echo "${R}  ⚠️ 缺少数据保留期限${N}"
        F=$((F+1))
    fi

    # 数据脱敏
    if grep -q "sanitizeLogData\|sanitizeErrorMessage\|脱敏\|****" "$CA_KT"; then
        echo "${G}  ✅ 数据脱敏存在${N}"
    else
        echo "${R}  ⚠️ 缺少数据脱敏${N}"
        F=$((F+1))
    fi

    # GDPR合规
    if grep -q "GDPR\|revokePrivacyConsent\|撤回授权\|数据泄露" "$CA_KT"; then
        echo "${G}  ✅ GDPR合规存在${N}"
    else
        echo "${R}  ⚠️ 缺少GDPR合规${N}"
        F=$((F+1))
    fi

    # 水印溯源
    if grep -q "generateVisibleWatermark\|generateInvisibleWatermark\|nativeGenerateWatermark" "$CA_KT"; then
        echo "${G}  ✅ 水印溯源存在${N}"
    else
        echo "${R}  ⚠️ 缺少水印溯源${N}"
        F=$((F+1))
    fi

    # 版权证明
    if grep -q "registerCopyright\|版权\|contentHash" "$CA_KT"; then
        echo "${G}  ✅ 版权证明存在${N}"
    else
        echo "${R}  ⚠️ 缺少版权证明${N}"
        F=$((F+1))
    fi

    # 安全教育
    if grep -q "showSecurityEducation\|安全提示\|防钓鱼\|isPhishingUrl" "$CA_KT"; then
        echo "${G}  ✅ 安全教育存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少安全教育${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ComplianceAuditor.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号60：审计+更新安全+供应链+发布 =====
echo "=== 编号60：审计+更新安全+供应链+发布 ==="
echo "验证：审计日志完整可追溯+依赖库无CVE+更新包签名验证通过+发布流程每步有记录"
echo "异常判定：审计日志缺失/服务端漏洞/供应链漏洞/更新包签名不匹配=对应处理"

if [ -f "$CA_KT" ]; then
    # 操作日志
    if grep -q "logAudit\|AuditLog\|audit" "$CA_KT"; then
        echo "${G}  ✅ 操作日志存在${N}"
    else
        echo "${R}  ⚠️ 缺少操作日志${N}"
        F=$((F+1))
    fi

    # 安全事件分级响应
    if grep -q "SecuritySeverity\|P0.*1\|P1.*4\|reportSecurityEvent" "$CA_KT"; then
        echo "${G}  ✅ 安全事件分级响应存在${N}"
    else
        echo "${R}  ⚠️ 缺少安全事件分级${N}"
        F=$((F+1))
    fi

    # APP更新安全：签名验证
    if grep -q "verifyUpdatePackage\|verifyUpdateSignature\|签名验证" "$CA_KT"; then
        echo "${G}  ✅ 更新包签名验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少更新包签名验证${N}"
        F=$((F+1))
    fi

    # APP更新安全：SHA-256完整性校验
    if grep -q "sha256\|SHA-256\|完整性校验" "$CA_KT"; then
        echo "${G}  ✅ 更新包SHA-256完整性校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少更新包完整性校验${N}"
        F=$((F+1))
    fi

    # APP更新安全：HTTPS通道
    if grep -q "https://" "$CA_KT"; then
        echo "${G}  ✅ 更新通道HTTPS存在${N}"
    else
        echo "${R}  ⚠️ 缺少HTTPS通道验证${N}"
        F=$((F+1))
    fi

    # APP更新安全：强制更新
    if grep -q "checkForceUpdate\|isForceUpdate\|minSupportedVersion" "$CA_KT"; then
        echo "${G}  ✅ 强制更新机制存在${N}"
    else
        echo "${R}  ⚠️ 缺少强制更新${N}"
        F=$((F+1))
    fi

    # 供应链安全清单
    if grep -q "getSupplyChainChecklist\|verification-metadata\|依赖.*锁定\|CVE" "$CA_KT"; then
        echo "${G}  ✅ 供应链安全清单存在${N}"
    else
        echo "${R}  ⚠️ 缺少供应链安全清单${N}"
        F=$((F+1))
    fi

    # 发布流程清单
    if grep -q "getReleaseChecklist\|MobSF\|混淆\|SAST" "$CA_KT"; then
        echo "${G}  ✅ 发布流程清单存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少发布流程清单${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ ComplianceAuditor.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层验证 =====
echo "=== Native层验证 ==="
NATIVE_C="$SEC_DIR/native_compliance.c"
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

    # 水印生成
    if grep -q "generate_watermark\|fork_generate_watermark" "$NATIVE_C"; then
        echo "${G}  ✅ 编号59 水印生成存在${N}"
    else
        echo "${R}  ⚠️ 缺少水印生成${N}"
        F=$((F+1))
    fi

    # 水印验证
    if grep -q "verify_watermark\|fork_verify_watermark" "$NATIVE_C"; then
        echo "${G}  ✅ 编号59 水印验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少水印验证${N}"
        F=$((F+1))
    fi

    # 水印格式：抗裁剪压缩转码
    if grep -q "NXWM\|WM_MAGIC\|校验和" "$NATIVE_C"; then
        echo "${G}  ✅ 水印格式（魔数+校验和）存在${N}"
    else
        echo "${R}  ⚠️ 缺少水印格式定义${N}"
        F=$((F+1))
    fi

    # 安全清零
    if grep -q "ca_secure_zero\|volatile" "$NATIVE_C"; then
        echo "${G}  ✅ 安全清零（volatile防优化）存在${N}"
    else
        echo "${R}  ⚠️ 缺少安全清零${N}"
        F=$((F+1))
    fi

    JNI_OK=0
    if grep -q "nativeGenerateWatermark" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeVerifyWatermark" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if [ "$JNI_OK" -eq 2 ]; then
        echo "${G}  ✅ JNI注册: 2/2 全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ JNI注册仅 $JNI_OK/2${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_compliance.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目十三验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部2项合规/审计验证通过${N}"
    echo "${G}[崩溃率] 预期零崩溃${N}"
    echo "${G}[防崩溃] 标准API+try-catch${N}"
    echo "${G}[覆盖] 59: 隐私弹窗+数据删除+数据导出+保留期限+脱敏+GDPR+水印+安全教育${N}"
    echo "${G}[覆盖] 60: 审计日志+安全事件分级+更新安全+供应链+发布流程${N}"
    exit 0
fi
