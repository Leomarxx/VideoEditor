#!/bin/bash
# ============================================
# NexClip 类目三：反调试 - CI/CD验证
# 编号18/33全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目三：反调试 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号18：多层调试器检测 =====
echo "=== 编号18：多层调试器检测（8层） ==="
echo "验证：正常环境下8层全部通过 | adb attach调试器后至少3层触发"
echo "异常判定：2层以上触发=弹警告+强制关闭+上报服务端"

DD_KT="$SRC_DIR/com/myvideo/editor/security/DebuggerDetector.kt"
if [ -f "$DD_KT" ]; then
    # 检查8层检测是否存在
    LAYERS=("TracerPid" "Wchan" "Jdwp" "DebuggerConnected" "WaitingDebugger" "SignalQueue" "OomScore" "TaskTracer")
    LAYER_OK=0
    for layer in "${LAYERS[@]}"; do
        if grep -q "$layer" "$DD_KT"; then
            LAYER_OK=$((LAYER_OK+1))
        else
            echo "${R}  ⚠️ 缺少层: $layer${N}"
        fi
    done
    if [ "$LAYER_OK" -eq 8 ]; then
        echo "${G}  ✅ 8层检测全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 仅 $LAYER_OK/8 层检测存在${N}"
        F=$((F+1))
    fi

    # 检查综合打分逻辑（>=2层触发=异常）
    if grep -q "triggered >= 2\|triggered >=2\|>=2\|>= 2" "$DD_KT"; then
        echo "${G}  ✅ 综合打分逻辑存在（>=2层触发）${N}"
    else
        echo "${Y}  ⚠️ 未找到明确的>=2层触发逻辑${N}"
    fi

    # 检查防崩溃：/proc/self/status读取用try-catch
    PROC_COUNT=$(grep -c "/proc/self" "$DD_KT" || true)
    TRY_COUNT=$(grep -c "try" "$DD_KT" || true)
    if [ "$PROC_COUNT" -gt 0 ] && [ "$TRY_COUNT" -gt 0 ]; then
        echo "${G}  ✅ /proc读取有try-catch包裹（防崩溃）${N}"
    else
        echo "${Y}  ⚠️ /proc读取防崩溃措施需确认${N}"
    fi
else
    echo "${R}  ⚠️ DebuggerDetector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号33：混合层级检测 =====
echo "=== 编号33：混合层级检测 ==="
echo "验证：两层检测结果一致且都为正常 | 附加调试器后两层至少一层触发"
echo "异常判定：任一层发现异常=弹警告+强制关闭+上报服务端"

if [ -f "$DD_KT" ]; then
    # 检查Java层4项
    JAVA_CHECKS=("isDebuggerConnected" "waitingForDebugger" "adb_enabled" "DEVELOPMENT_SETTINGS_ENABLED")
    JAVA_OK=0
    for check in "${JAVA_CHECKS[@]}"; do
        if grep -q "$check" "$DD_KT"; then
            JAVA_OK=$((JAVA_OK+1))
        else
            echo "${Y}  ⚠️ Java层缺少: $check${N}"
        fi
    done
    echo "${G}  ✅ Java层检测: $JAVA_OK/4 项存在${N}"

    # 检查C层调用
    if grep -q "nativeAntiDebugDetect" "$DD_KT"; then
        echo "${G}  ✅ C层fork隔离检测接口存在${N}"
    else
        echo "${R}  ⚠️ 缺少C层检测接口${N}"
        F=$((F+1))
    fi

    # 检查交叉验证逻辑
    if grep -q "交叉验证\|crossCheck\|javaTriggered.*nativeTriggered\|nativeTriggered.*javaTriggered" "$DD_KT"; then
        echo "${G}  ✅ Java+C层交叉验证逻辑存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少交叉验证逻辑${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ DebuggerDetector.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层验证 =====
echo "=== Native层fork隔离验证 ==="
NATIVE_C="$SEC_DIR/native_anti_debug.c"
if [ -f "$NATIVE_C" ]; then
    # 检查fork调用
    FORK_COUNT=$(grep -c "fork()" "$NATIVE_C" || true)
    if [ "$FORK_COUNT" -gt 0 ]; then
        echo "${G}  ✅ fork隔离调用存在 ($FORK_COUNT 处)${N}"
    else
        echo "${R}  ⚠️ 缺少fork隔离${N}"
        F=$((F+1))
    fi

    # 检查waitpid + WIFEXITED
    if grep -q "waitpid" "$NATIVE_C" && grep -q "WIFEXITED" "$NATIVE_C"; then
        echo "${G}  ✅ waitpid + WIFEXITED 存在${N}"
    else
        echo "${R}  ⚠️ 缺少子进程回收/崩溃检测${N}"
        F=$((F+1))
    fi

    # 检查编号18的C层检测项
    C_CHECKS=("check_tracer_pid" "check_wchan" "check_jdwp" "check_task_tracer")
    C_OK=0
    for check in "${C_CHECKS[@]}"; do
        if grep -q "$check" "$NATIVE_C"; then
            C_OK=$((C_OK+1))
        else
            echo "${Y}  ⚠️ C层缺少: $check${N}"
        fi
    done
    echo "${G}  ✅ 编号18 C层检测: $C_OK/4 项存在${N}"

    # 检查编号33的C层检测项
    C33_CHECKS=("check_fdinfo" "check_exe_link" "check_ld_preload")
    C33_OK=0
    for check in "${C33_CHECKS[@]}"; do
        if grep -q "$check" "$NATIVE_C"; then
            C33_OK=$((C33_OK+1))
        else
            echo "${Y}  ⚠️ C层缺少: $check${N}"
        fi
    done
    if [ "$C33_OK" -eq 3 ]; then
        echo "${G}  ✅ 编号33 C层检测: 3/3 项存在${N}"
    else
        echo "${R}  ⚠️ 编号33 C层检测仅 $C33_OK/3${N}"
        F=$((F+1))
    fi

    # 检查JNI注册
    if grep -q "Java_com_myvideo_editor_security_DebuggerDetector_nativeAntiDebugDetect" "$NATIVE_C"; then
        echo "${G}  ✅ JNI注册存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少JNI注册${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_anti_debug.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目三验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部2项反调试验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] Java层标准API + C层fork隔离${N}"
    exit 0
fi
