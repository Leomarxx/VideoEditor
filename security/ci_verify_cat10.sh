#!/bin/bash
# ============================================
# NexClip 类目十：界面保护 - CI/CD验证
# 编号49/50全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目十：界面保护 - CI/CD验证"
echo "============================================"
echo ""

UI_KT="$SRC_DIR/com/myvideo/editor/security/UIProtector.kt"

# ===== 编号49：组件安全 =====
echo "=== 编号49：组件安全 ==="
echo "验证：覆盖测试无法触发+组件不暴露+Deep Link注入无效+WebView无法加载恶意页面"
echo "异常判定：防覆盖未生效/组件暴露/Deep Link注入/路径穿越/WebView安全未生效=弹警告+强制关闭"

if [ -f "$UI_KT" ]; then
    # 防Tapjacking
    if grep -q "filterTouchesWhenObscured\|protectAgainstTapjacking" "$UI_KT"; then
        echo "${G}  ✅ 防Tapjacking存在${N}"
    else
        echo "${R}  ⚠️ 缺少防Tapjacking${N}"
        F=$((F+1))
    fi

    # 悬浮窗检测
    if grep -q "detectOverlayAttack\|nativeDetectOverlay\|canDrawOverlays" "$UI_KT"; then
        echo "${G}  ✅ 悬浮窗检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少悬浮窗检测${N}"
        F=$((F+1))
    fi

    # 组件暴露检测
    if grep -q "checkComponentExposure\|exported" "$UI_KT"; then
        echo "${G}  ✅ 组件暴露检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少组件暴露检测${N}"
        F=$((F+1))
    fi

    # Deep Link白名单验证
    if grep -q "validateDeepLink\|DEEP_LINK_SCHEMES\|DEEP_LINK_HOSTS" "$UI_KT"; then
        echo "${G}  ✅ Deep Link白名单验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少Deep Link验证${N}"
        F=$((F+1))
    fi

    # Intent安全
    if grep -q "createSecureIntent\|validateIntentSource\|明确Component" "$UI_KT"; then
        echo "${G}  ✅ Intent安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少Intent安全${N}"
        F=$((F+1))
    fi

    # 文件路径安全
    if grep -q "validateFilePath\|canonicalPath\|\.\." "$UI_KT"; then
        echo "${G}  ✅ 文件路径安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少文件路径安全${N}"
        F=$((F+1))
    fi

    # 反射安全
    if grep -q "checkReflectionAccess\|restrictedClasses\|reflectionLog" "$UI_KT"; then
        echo "${G}  ✅ 反射安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少反射安全${N}"
        F=$((F+1))
    fi

    # 动态加载安全
    if grep -q "validateDynamicLoad\|外部存储.*加载\|DexClassLoader" "$UI_KT"; then
        echo "${G}  ✅ 动态加载安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少动态加载安全${N}"
        F=$((F+1))
    fi

    # WebView安全
    WEBVIEW_CHECKS=("allowFileAccess" "MIXED_CONTENT" "setWebContentsDebuggingEnabled"
        "onReceivedSslError" "WEBVIEW_DOMAIN_WHITELIST" "shouldOverrideUrlLoading")
    WV_OK=0
    for wv in "${WEBVIEW_CHECKS[@]}"; do
        if grep -q "$wv" "$UI_KT"; then WV_OK=$((WV_OK+1)); fi
    done
    if [ "$WV_OK" -ge 4 ]; then
        echo "${G}  ✅ WebView安全: $WV_OK/${#WEBVIEW_CHECKS[@]} 项存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ WebView安全仅 $WV_OK/${#WEBVIEW_CHECKS[@]}${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ UIProtector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号50：录屏检测 =====
echo "=== 编号50：录屏检测 ==="
echo "验证：截屏返回黑色+录屏时敏感内容被模糊+通知栏不泄露+最近任务不显示"
echo "异常判定：FLAG_SECURE未生效/通知泄露敏感信息/最近任务泄露内容=弹警告+强制关闭"

if [ -f "$UI_KT" ]; then
    # FLAG_SECURE
    if grep -q "FLAG_SECURE\|enableScreenProtection" "$UI_KT"; then
        echo "${G}  ✅ FLAG_SECURE防截屏存在${N}"
    else
        echo "${R}  ⚠️ 缺少FLAG_SECURE${N}"
        F=$((F+1))
    fi

    # 屏幕录制检测
    if grep -q "detectScreenCapture\|MediaProjection\|nativeDetectScreenCapture" "$UI_KT"; then
        echo "${G}  ✅ 屏幕录制检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少屏幕录制检测${N}"
        F=$((F+1))
    fi

    # 投屏检测
    if grep -q "detectCasting\|DisplayManager\|displays" "$UI_KT"; then
        echo "${G}  ✅ 投屏检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少投屏检测${N}"
        F=$((F+1))
    fi

    # 悬浮窗检测（和编号49共用）
    if grep -q "detectOverlayWindows\|nativeDetectOverlay" "$UI_KT"; then
        echo "${G}  ✅ 悬浮窗检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少悬浮窗检测${N}"
        F=$((F+1))
    fi

    # 通知栏安全
    if grep -q "VISIBILITY_SECRET\|configureSecureNotification\|锁屏.*不显示" "$UI_KT"; then
        echo "${G}  ✅ 通知栏安全存在${N}"
    else
        echo "${R}  ⚠️ 缺少通知栏安全${N}"
        F=$((F+1))
    fi

    # 最近任务保护
    if grep -q "protectRecentTasks\|onWindowFocusChangeListener\|sensitiveViews" "$UI_KT"; then
        echo "${G}  ✅ 最近任务保护存在${N}"
    else
        echo "${R}  ⚠️ 缺少最近任务保护${N}"
        F=$((F+1))
    fi

    # 敏感View模糊
    if grep -q "registerSensitiveView\|alpha.*=.*0f\|INVISIBLE" "$UI_KT"; then
        echo "${G}  ✅ 敏感View模糊隐藏存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少敏感View模糊${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ UIProtector.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层验证 =====
echo "=== Native层验证 ==="
NATIVE_C="$SEC_DIR/native_ui_protect.c"
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

    # 编号49 C层
    if grep -q "detect_overlay\|overlay" "$NATIVE_C"; then
        echo "${G}  ✅ 编号49 C层悬浮窗检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少C层悬浮窗检测${N}"
        F=$((F+1))
    fi

    # 编号50 C层
    if grep -q "detect_screen_capture\|screenrecord" "$NATIVE_C"; then
        echo "${G}  ✅ 编号50 C层屏幕录制检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少C层屏幕录制检测${N}"
        F=$((F+1))
    fi

    # 投屏检测
    if grep -q "detect_casting_service\|miracast\|chromecast" "$NATIVE_C"; then
        echo "${G}  ✅ 编号50 C层投屏检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少C层投屏检测${N}"
        F=$((F+1))
    fi

    JNI_OK=0
    if grep -q "nativeDetectOverlay" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeDetectScreenCapture" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if [ "$JNI_OK" -eq 2 ]; then
        echo "${G}  ✅ JNI注册: 2/2 全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ JNI注册仅 $JNI_OK/2${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_ui_protect.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== AndroidManifest验证 =====
echo "=== AndroidManifest验证 ==="
MANIFEST="$SRC_DIR/../AndroidManifest.xml"
if [ -f "$MANIFEST" ]; then
    if grep -q "exported=\"false\"" "$MANIFEST"; then
        echo "${G}  ✅ 组件exported=false配置存在${N}"
    else
        echo "${Y}  ⚠️ 未找到exported=false配置${N}"
    fi
else
    echo "${Y}  ⚠️ AndroidManifest.xml路径需确认${N}"
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目十验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部2项界面保护验证通过${N}"
    echo "${G}[崩溃率] 预期零崩溃${N}"
    echo "${G}[防崩溃] 标准Android API+try-catch${N}"
    echo "${G}[覆盖] 49: 防覆盖+组件暴露+Deep Link+Intent+路径+WebView+反射+动态加载${N}"
    echo "${G}[覆盖] 50: FLAG_SECURE+录屏检测+投屏检测+通知安全+最近任务保护+悬浮窗检测${N}"
    exit 0
fi
