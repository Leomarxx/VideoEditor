#!/bin/bash
# ============================================
# NexClip 编号8：符号表Strip
# 做什么：去除SO文件中所有符号信息，让IDA/Ghidra只能看到sub_xxxxxxx格式
# 程度：strip --strip-all，去除.debug/.comment/.note section，-s，
#       编译后llvm-strip再strip一次（双重保险）
# 验证方式：readelf -s确认无导出符号 | readelf -S确认无.debug/.comment/.note |
#           IDA打开只看到sub_xxxxxxx
# 异常判定：CI/CD中readelf确认无敏感符号残留→发现则构建失败
# 崩溃率：零
# ============================================

set -e

SO_DIR="${1:-app/build/intermediates/merged_native_libs}"
NDK_STRIP="${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
SYSTEM_STRIP="strip"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "============================================"
echo " 编号8：符号表Strip（双重保险）"
echo "============================================"

SO_FILES=$(find "$SO_DIR" -name "*.so" -type f 2>/dev/null)

if [ -z "$SO_FILES" ]; then
    echo "${RED}[异常判定] 未找到SO文件→构建失败${NC}"
    exit 1
fi

STRIPPED_COUNT=0

for SO in $SO_FILES; do
    echo "[处理] $(basename $SO)"

    # 第一轮：系统strip --strip-all
    if command -v $SYSTEM_STRIP &> /dev/null; then
        $SYSTEM_STRIP --strip-all "$SO" 2>/dev/null || true
        echo "  [1/2] 系统strip完成"
    fi

    # 第二轮：llvm-strip --strip-all（双重保险）
    if [ -f "$NDK_STRIP" ]; then
        "$NDK_STRIP" --strip-all "$SO" 2>/dev/null || true
        echo "  [2/2] llvm-strip完成"
    fi

    STRIPPED_COUNT=$((STRIPPED_COUNT + 1))
done

echo ""
echo "============================================"
echo " 验证阶段"
echo "============================================"

FAIL=0

for SO in $SO_FILES; do
    echo "[验证] $(basename $SO)"

    # 验证方式1：readelf -s确认无导出符号
    EXPORTS=$(readelf -s "$SO" 2>/dev/null | grep -c "GLOBAL" || true)
    if [ "$EXPORTS" -gt 0 ]; then
        echo "${RED}  ⚠️ 发现 $EXPORTS 个GLOBAL符号${NC}"
        readelf -s "$SO" 2>/dev/null | grep "GLOBAL" | head -5
        FAIL=1
    else
        echo "${GREEN}  ✅ 无导出符号${NC}"
    fi

    # 验证方式2：readelf -S确认无.debug/.comment/.note
    DEBUG_SECTIONS=$(readelf -S "$SO" 2>/dev/null | grep -cE "\.debug|\.comment|\.note" || true)
    if [ "$DEBUG_SECTIONS" -gt 0 ]; then
        echo "${RED}  ⚠️ 发现 $DEBUG_SECTIONS 个调试section${NC}"
        readelf -S "$SO" 2>/dev/null | grep -E "\.debug|\.comment|\.note"
        FAIL=1
    else
        echo "${GREEN}  ✅ 无.debug/.comment/.note section${NC}"
    fi
done

# 异常判定
if [ "$FAIL" -eq 1 ]; then
    echo ""
    echo "${RED}[异常判定] CI/CD readelf确认有敏感符号残留→构建失败${NC}"
    exit 1
else
    echo ""
    echo "${GREEN}[验证通过] 共处理 $STRIPPED_COUNT 个SO文件${NC}"
    echo "${GREEN}[验证通过] IDA打开将只看到sub_xxxxxxx格式${NC}"
    echo "${GREEN}[崩溃率] 预期零${NC}"
    exit 0
fi
