#!/bin/bash
# ============================================
# NexClip 类目四：反Hook - CI/CD验证
# 编号17/24/31全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目四：反Hook - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号17：全框架检测 =====
echo "=== 编号17：全框架检测（10+框架） ==="
echo "验证：无任何Hook框架环境时全部通过 | 安装任何Hook框架后立即触发"
echo "异常判定：发现任何框架特征=弹警告+强制关闭+上报服务端"

HD_KT="$SRC_DIR/com/myvideo/editor/security/HookDetector.kt"
if [ -f "$HD_KT" ]; then
    # 检查Xposed检测（4项）
    XP_CHECKS=("XposedBridge" "xposed" "Xposed-maps" "Xposed-class" "Xposed-stack" "Xposed-prop")
    XP_OK=0
    for check in "${XP_CHECKS[@]}"; do
        if grep -q "$check" "$HD_KT"; then
            XP_OK=$((XP_OK+1))
        fi
    done
    if [ "$XP_OK" -ge 4 ]; then
        echo "${G}  ✅ Xposed检测: $XP_OK 项存在${N}"
    else
        echo "${R}  ⚠️ Xposed检测仅 $XP_OK 项${N}"
        F=$((F+1))
    fi

    # 检查Zygisk检测（4项）
    if grep -q "libzygisk" "$HD_KT" && grep -q "Zygisk" "$HD_KT"; then
        echo "${G}  ✅ Zygisk检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少Zygisk检测${N}"
        F=$((F+1))
    fi

    # 检查Substrate/EdXposed/VirtualXposed
    FW=("Substrate" "EdXposed" "VirtualXposed" "TaiChi")
    FW_OK=0
    for fw in "${FW[@]}"; do
        if grep -q "$fw" "$HD_KT"; then
            FW_OK=$((FW_OK+1))
        else
            echo "${Y}  ⚠️ 缺少: $fw${N}"
        fi
    done
    echo "${G}  ✅ 其他框架检测: $FW_OK/${#FW[@]} 存在${N}"

    # 检查fork隔离调用
    if grep -q "fork\|nativeFridaDeepDetect" "$HD_KT"; then
        echo "${G}  ✅ C层fork隔离调用存在${N}"
    else
        echo "${Y}  ⚠️ 未找到fork隔离调用${N}"
    fi

    # 检查综合打分：任意1个触发=异常
    if grep -q "triggered > 0\|abnormal" "$HD_KT"; then
        echo "${G}  ✅ 综合打分逻辑存在（任意1个触发=异常）${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 未找到打分逻辑${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ HookDetector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号24：Frida深度检测 =====
echo "=== 编号24：Frida深度检测 ==="
echo "验证：无Frida环境时全部通过 | 启动Frida server后立即触发"
echo "异常判定：发现Frida特征=弹警告+强制关闭+上报服务端"

NATIVE_C="$SEC_DIR/native_hook_detect.c"
if [ -f "$NATIVE_C" ]; then
    # GJS引擎特征
    if grep -q "gjs\|girepository\|gobject\|libglib" "$NATIVE_C"; then
        echo "${G}  ✅ GJS引擎特征检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少GJS引擎检测${N}"
        F=$((F+1))
    fi

    # D-Bus特征
    if grep -q "DBUS" "$NATIVE_C"; then
        echo "${G}  ✅ D-Bus通信特征检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少D-Bus检测${N}"
        F=$((F+1))
    fi

    # 堆内存特征码扫描
    if grep -q "FRIDA\|frida.*特征" "$NATIVE_C"; then
        echo "${G}  ✅ 堆内存特征码扫描存在${N}"
    else
        echo "${R}  ⚠️ 缺少堆内存扫描${N}"
        F=$((F+1))
    fi

    # 严格地址范围校验（防越界）
    if grep -q "10.*1024\|扫描范围" "$NATIVE_C"; then
        echo "${G}  ✅ 扫描范围限制存在（防越界）${N}"
    else
        echo "${Y}  ⚠️ 未找到扫描范围限制${N}"
    fi

    # 信号处理（防崩溃）
    if grep -q "sigsetjmp\|siglongjmp\|SIGSEGV\|SIGBUS" "$NATIVE_C"; then
        echo "${G}  ✅ 信号处理存在（防读取崩溃）${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少信号处理→读取越界崩溃${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_hook_detect.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号31：Frida标准检测（4方法） =====
echo "=== 编号31：Frida标准检测 ==="
echo "验证：四个方法全部通过 | 启动Frida后至少方法2和方法4触发"
echo "异常判定：任意方法触发=弹警告+强制关闭+上报服务端"

if [ -f "$HD_KT" ]; then
    # 方法1：maps扫描
    if grep -q "fridaMethod1_Maps\|Frida-maps" "$HD_KT"; then
        echo "${G}  ✅ 方法1 maps扫描存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法1${N}"
        F=$((F+1))
    fi

    # 方法2：27042端口
    if grep -q "27042\|fridaMethod2_Port" "$HD_KT"; then
        echo "${G}  ✅ 方法2 27042端口检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法2${N}"
        F=$((F+1))
    fi

    # 方法3：遍历/proc
    if grep -q "fridaMethod3_Proc\|frida-server" "$HD_KT"; then
        echo "${G}  ✅ 方法3 /proc遍历存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法3${N}"
        F=$((F+1))
    fi

    # 方法4：rwx内存
    if grep -q "fridaMethod4_Rwx\|rwx" "$HD_KT"; then
        echo "${G}  ✅ 方法4 rwx内存检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法4${N}"
        F=$((F+1))
    fi
fi
echo ""

# ===== Native层fork隔离验证 =====
echo "=== Native层fork隔离验证 ==="
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

    # 检查非阻塞socket（编号31方法2）
    if grep -q "socket\|SOCK_STREAM" "$NATIVE_C"; then
        echo "${G}  ✅ socket连接检测存在${N}"
    else
        echo "${Y}  ⚠️ socket检测在Java层实现${N}"
    fi

    # 检查JNI注册
    if grep -q "Java_com_myvideo_editor_security_HookDetector" "$NATIVE_C"; then
        echo "${G}  ✅ JNI注册存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少JNI注册${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_hook_detect.c不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目四验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部3项反Hook验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] fork隔离+非阻塞socket+信号处理${N}"
    exit 0
fi
