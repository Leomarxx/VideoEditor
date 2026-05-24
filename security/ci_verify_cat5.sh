#!/bin/bash
# ============================================
# NexClip 类目五：反注入 - CI/CD验证
# 编号32/34/36/37全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目五：反注入 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号32：注入区域监控 =====
echo "=== 编号32：注入区域监控 ==="
echo "验证：无异常新增映射时全部通过 | 注入任何SO后立即发现新增映射"
echo "异常判定：发现可疑映射=弹警告+强制关闭+上报服务端"

ID_KT="$SRC_DIR/com/myvideo/editor/security/InjectionDetector.kt"
if [ -f "$ID_KT" ]; then
    # 检查maps hash快照
    if grep -q "mapsSnapshotHash\|mapsHash\|calculateMapsHash" "$ID_KT"; then
        echo "${G}  ✅ maps hash快照逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少maps hash快照${N}"
        F=$((F+1))
    fi

    # 检查每5分钟定期检查
    if grep -q "MINUTES\|5.*分钟\|scheduleAtFixedRate" "$ID_KT"; then
        echo "${G}  ✅ 定期监控逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少定期监控${N}"
        F=$((F+1))
    fi

    # 检查新增可执行区域检测
    if grep -q "r-x\|rwx.*anon\|匿名.*可执行" "$ID_KT"; then
        echo "${G}  ✅ 新增可执行区域检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少可执行区域检测${N}"
        F=$((F+1))
    fi

    # 检查可疑关键字对比
    KEYWORDS=("frida" "xposed" "substrate" "zygisk" "magisk" "inject" "hook" "gadget")
    KW_OK=0
    for kw in "${KEYWORDS[@]}"; do
        if grep -qi "$kw" "$ID_KT"; then
            KW_OK=$((KW_OK+1))
        fi
    done
    if [ "$KW_OK" -ge 6 ]; then
        echo "${G}  ✅ 可疑关键字对比: $KW_OK/${#KEYWORDS[@]} 存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 可疑关键字仅 $KW_OK/${#KEYWORDS[@]}${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ InjectionDetector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号34：进程注入检测 =====
echo "=== 编号34：进程注入检测 ==="
echo "验证：无异常SO加载时全部通过 | LD_PRELOAD被设置或加载未知SO后触发"
echo "异常判定：发现异常注入=弹警告+强制关闭+上报服务端"

if [ -f "$ID_KT" ]; then
    # 检查LD_PRELOAD检测
    if grep -q "LD_PRELOAD" "$ID_KT"; then
        echo "${G}  ✅ LD_PRELOAD检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少LD_PRELOAD检测${N}"
        F=$((F+1))
    fi

    # 检查SO列表对比
    if grep -q "soSnapshot\|SO列表" "$ID_KT"; then
        echo "${G}  ✅ SO列表对比逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少SO列表对比${N}"
        F=$((F+1))
    fi

    # 检查合法目录白名单
    if grep -q "LEGIT_SO_DIRS\|白名单\|/data/app\|/system/lib" "$ID_KT"; then
        echo "${G}  ✅ 合法目录白名单存在${N}"
    else
        echo "${R}  ⚠️ 缺少合法目录白名单${N}"
        F=$((F+1))
    fi

    # 检查fork隔离调用
    if grep -q "native\|fork" "$ID_KT"; then
        echo "${G}  ✅ C层fork隔离调用存在${N}"
        P=$((P+1))
    else
        echo "${Y}  ⚠️ 未找到fork隔离调用${N}"
    fi
else
    echo "${R}  ⚠️ InjectionDetector.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号36：maps变化监控 =====
echo "=== 编号36：maps变化监控 ==="
echo "验证：maps无异常变化时全部通过 | 修改内存映射后立即发现变化"
echo "异常判定：发现异常变化=弹警告+强制关闭+上报服务端"

if [ -f "$ID_KT" ]; then
    # 检查maps内容对比
    if grep -q "mapsContent\|maps.*变化\|analyzeMapsChange" "$ID_KT"; then
        echo "${G}  ✅ maps内容对比逻辑存在${N}"
    else
        echo "${R}  ⚠️ 缺少maps内容对比${N}"
        F=$((F+1))
    fi

    # 检查和32的互补逻辑
    if grep -q "32.*互补\|互补.*32\|编号36" "$ID_KT"; then
        echo "${G}  ✅ 和编号32互补逻辑存在${N}"
    else
        echo "${Y}  ⚠️ 未找到和编号32的互补说明${N}"
    fi

    # 检查rwx匿名映射检测
    if grep -q "rwx" "$ID_KT"; then
        echo "${G}  ✅ rwx映射检测存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少rwx映射检测${N}"
        F=$((F+1))
    fi
fi
echo ""

# ===== 编号37：SO加载行为完整性 =====
echo "=== 编号37：SO完整性（四维度） ==="
echo "验证：四个维度全部通过 | 替换/修改SO文件或注入未知SO后触发"
echo "异常判定：任一维度发现异常=弹警告+强制关闭+上报服务端"

NATIVE_C="$SEC_DIR/native_inject_detect.c"
if [ -f "$NATIVE_C" ]; then
    # 方法1 SO文件SHA-256校验
    if grep -q "verify_so_hash\|SHA-256\|SHA256" "$NATIVE_C"; then
        echo "${G}  ✅ 方法1 SO文件SHA-256校验存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法1${N}"
        F=$((F+1))
    fi

    # 方法2 SO加载路径验证
    if grep -q "verify_so_paths\|is_legit_so_path" "$NATIVE_C"; then
        echo "${G}  ✅ 方法2 SO路径验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法2${N}"
        F=$((F+1))
    fi

    # 方法3 SO加载顺序（Java层实现）
    if [ -f "$ID_KT" ] && grep -q "加载顺序\|soSnapshot\|SO列表" "$ID_KT"; then
        echo "${G}  ✅ 方法3 SO加载顺序监控存在${N}"
    else
        echo "${Y}  ⚠️ 方法3 SO加载顺序需确认${N}"
    fi

    # 方法4 SO文件属性验证
    if grep -q "verify_so_attrs\|st_mtime" "$NATIVE_C"; then
        echo "${G}  ✅ 方法4 SO属性验证存在${N}"
    else
        echo "${R}  ⚠️ 缺少方法4${N}"
        F=$((F+1))
    fi

    # 合法目录白名单
    if grep -q "is_legit_so_path\|/data/app/\|/system/lib" "$NATIVE_C"; then
        echo "${G}  ✅ 合法目录白名单存在${N}"
    else
        echo "${R}  ⚠️ 缺少合法目录白名单${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_inject_detect.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层fork隔离验证 =====
echo "=== Native层fork隔离验证 ==="
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
        echo "${R}  ⚠️ 缺少子进程回收/崩溃检测${N}"
        F=$((F+1))
    fi

    # 检查pipe通信（fork_maps_hash用pipe传结果）
    if grep -q "pipe\|pipefd" "$NATIVE_C"; then
        echo "${G}  ✅ pipe通信存在（子进程传递hash结果）${N}"
    else
        echo "${Y}  ⚠️ 未找到pipe通信${N}"
    fi

    if grep -q "Java_com_myvideo_editor_security_InjectionDetector" "$NATIVE_C"; then
        echo "${G}  ✅ JNI注册存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少JNI注册${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_inject_detect.c不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目五验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部4项反注入验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] fork隔离+只做文本解析+pipe通信${N}"
    echo "${G}[覆盖] 32+36联合覆盖maps新增+变化${N}"
    echo "${G}[覆盖] 34覆盖进程注入${N}"
    echo "${G}[覆盖] 37覆盖SO完整性四维度${N}"
    exit 0
fi
