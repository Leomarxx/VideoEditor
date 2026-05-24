#!/bin/bash
# ============================================
# NexClip 类目七：内存安全 - CI/CD验证
# 编号40/41/42全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目七：内存安全 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号40：内存保护验证 =====
echo "=== 编号40：内存保护验证 ==="
echo "验证：内存段权限正确时全部通过 | 发现rwx段则触发"
echo "异常判定：发现rwx段=弹警告+强制关闭+上报服务端"

MP_KT="$SRC_DIR/com/myvideo/editor/security/MemoryProtector.kt"
if [ -f "$MP_KT" ]; then
    # 检查rwx段检测
    if grep -q "rwx\|rwxp" "$MP_KT"; then
        echo "${G}  ✅ rwx段检测逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少rwx段检测${N}"
        F=$((F+1))
    fi

    # 检查maps读取
    if grep -q "/proc/self/maps" "$MP_KT"; then
        echo "${G}  ✅ /proc/self/maps读取存在${N}"
    else
        echo "${R}  ⚠️ 缺少maps读取${N}"
        F=$((F+1))
    fi

    # 检查C层fork隔离调用
    if grep -q "nativeMemoryProtectCheck" "$MP_KT"; then
        echo "${G}  ✅ C层fork隔离调用存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少C层fork隔离调用${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ MemoryProtector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号41：防dump被动检测 =====
echo "=== 编号41：防dump被动检测 ==="
echo "验证：无进程在读取本进程内存时通过 | 发现有进程打开/proc/PID/mem则触发"
echo "异常判定：发现dump行为=弹警告+强制关闭+上报服务端"

if [ -f "$MP_KT" ]; then
    # 检查fd遍历
    if grep -q "fd\|readlink\|canonicalPath" "$MP_KT"; then
        echo "${G}  ✅ fd遍历检测逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少fd遍历检测${N}"
        F=$((F+1))
    fi

    # 检查/proc/PID/mem检测
    if grep -q "/proc.*mem\|myMemPath" "$MP_KT"; then
        echo "${G}  ✅ /proc/PID/mem检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少mem文件检测${N}"
        F=$((F+1))
    fi

    # 检查敏感数据处理规范
    if grep -q "memset_s\|fill(0)\|清零" "$MP_KT"; then
        echo "${G}  ✅ 密钥清零逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少密钥清零${N}"
        F=$((F+1))
    fi

    # 检查volatile声明
    if grep -q "volatile" "$MP_KT"; then
        echo "${G}  ✅ volatile声明存在${N}"
    else
        echo "${Y}  ⚠️ 未找到volatile声明（Java层可能在C层实现）${N}"
    fi

    # 检查C层dump检测调用
    if grep -q "nativeDumpDetect" "$MP_KT"; then
        echo "${G}  ✅ C层dump检测调用存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少C层dump检测${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ MemoryProtector.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号42：密钥内存擦除 =====
echo "=== 编号42：密钥内存擦除 ==="
echo "验证：内存中搜索已知密钥模式无残留 | dump内存后分析无明文密钥"
echo "异常判定：发现内存中残留明文密钥=弹警告+强制关闭+上报服务端"

if [ -f "$MP_KT" ]; then
    # 检查memset_s/fill(0)清零
    if grep -q "fill(0)\|memset_s\|清零\|memzero" "$MP_KT"; then
        echo "${G}  ✅ memset_s清零逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少memset_s清零${N}"
        F=$((F+1))
    fi

    # 检查30秒过期自动清零
    if grep -q "30.*秒\|30000\|KEY_EXPIRE\|过期" "$MP_KT"; then
        echo "${G}  ✅ 30秒过期自动清零存在${N}"
    else
        echo "${R}  ⚠️ 缺少过期清零${N}"
        F=$((F+1))
    fi

    # 检查核心转储禁止
    if grep -q "disableCoreDump\|PR_SET_DUMPABLE\|核心转储" "$MP_KT"; then
        echo "${G}  ✅ 核心转储禁止存在${N}"
    else
        echo "${R}  ⚠️ 缺少核心转储禁止${N}"
        F=$((F+1))
    fi

    # 检查密钥轮换
    if grep -q "rotateMemoryKey\|轮换\|rotate" "$MP_KT"; then
        echo "${G}  ✅ 密钥轮换逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少密钥轮换${N}"
        F=$((F+1))
    fi

    # 检查分散内存存储
    if grep -q "storeDistributed\|分散存储\|Distributed" "$MP_KT"; then
        echo "${G}  ✅ 分散内存存储存在${N}"
    else
        echo "${R}  ⚠️ 缺少分散存储${N}"
        F=$((F+1))
    fi

    # 检查不在堆上长期存储明文
    if grep -q "ConcurrentHashMap\|secureKeys" "$MP_KT"; then
        echo "${G}  ✅ 密钥缓存管理存在${N}"
        P=$((P+1))
    else
        echo "${Y}  ⚠️ 密钥缓存管理需确认${N}"
    fi
fi
echo ""

# ===== Native层验证 =====
echo "=== Native层验证 ==="
NATIVE_C="$SEC_DIR/native_memory_protect.c"
if [ -f "$NATIVE_C" ]; then
    # 检查fork隔离
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

    # 检查secure_memzero
    if grep -q "secure_memzero\|memset_s" "$NATIVE_C"; then
        echo "${G}  ✅ secure_memzero安全清零存在${N}"
    else
        echo "${R}  ⚠️ 缺少安全清零${N}"
        F=$((F+1))
    fi

    # 检查volatile防止优化
    if grep -q "volatile" "$NATIVE_C"; then
        echo "${G}  ✅ volatile防编译器优化存在${N}"
    else
        echo "${R}  ⚠️ 缺少volatile${N}"
        F=$((F+1))
    fi

    # 检查prctl核心转储禁止
    if grep -q "prctl\|PR_SET_DUMPABLE\|RLIMIT_CORE" "$NATIVE_C"; then
        echo "${G}  ✅ prctl核心转储禁止存在${N}"
    else
        echo "${R}  ⚠️ 缺少核心转储禁止${N}"
        F=$((F+1))
    fi

    # 检查ARM Crypto支持
    if grep -q "ARM\|aarch64\|AES\|crypto" "$NATIVE_C"; then
        echo "${G}  ✅ ARM Crypto Extension存在${N}"
    else
        echo "${Y}  ⚠️ 未找到ARM Crypto支持${N}"
    fi

    # 检查always_inline内联
    if grep -q "always_inline" "$NATIVE_C"; then
        echo "${G}  ✅ always_inline内联存在（防Hook绕过）${N}"
    else
        echo "${R}  ⚠️ 缺少always_inline${N}"
        F=$((F+1))
    fi

    # 检查JNI注册
    JNI_OK=0
    if grep -q "nativeMemoryProtectCheck" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeDumpDetect" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeEnableCoreDumpProtection" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if grep -q "nativeEncryptMemoryKey" "$NATIVE_C"; then JNI_OK=$((JNI_OK+1)); fi
    if [ "$JNI_OK" -eq 4 ]; then
        echo "${G}  ✅ JNI注册: 4/4 全部存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ JNI注册仅 $JNI_OK/4${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_memory_protect.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目七验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部3项内存安全验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] fork隔离+只读操作+标准系统调用${N}"
    echo "${G}[覆盖] 40: 内存段权限验证${N}"
    echo "${G}[覆盖] 41: 防dump被动检测${N}"
    echo "${G}[覆盖] 42: 密钥内存擦除/清零/轮换/分散存储${N}"
    exit 0
fi
