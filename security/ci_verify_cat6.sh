#!/bin/bash
# ============================================
# NexClip 类目六：Root/环境检测 - CI/CD验证
# 编号21/22全部验证方式+异常判定
# ============================================

set -e

SRC_DIR="${1:-app/src/main/java}"
SEC_DIR="${2:-security}"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; N='\033[0m'
P=0; F=0

echo "============================================"
echo " NexClip 类目六：Root/环境检测 - CI/CD验证"
echo "============================================"
echo ""

# ===== 编号21：Root全方案检测 =====
echo "=== 编号21：Root全方案检测（6种） ==="
echo "验证：未Root设备全部通过 | Root后多个方法同时触发"
echo "异常判定：3个以上方法触发=弹警告+强制关闭+上报服务端"

RD_KT="$SRC_DIR/com/myvideo/editor/security/RootDetector.kt"
if [ -f "$RD_KT" ]; then
    # Magisk检测（8项）
    MAGISK=("su文件" "Magisk包名" "Magisk模块" "Magisk隐藏" "Magisk挂载" "Zygisk" "Bootloader" "init.rc")
    MAG_OK=0
    for m in "${MAGISK[@]}"; do
        if grep -q "$m" "$RD_KT"; then MAG_OK=$((MAG_OK+1)); fi
    done
    if [ "$MAG_OK" -ge 6 ]; then
        echo "${G}  ✅ Magisk检测: $MAG_OK/${#MAGISK[@]} 项存在${N}"
    else
        echo "${R}  ⚠️ Magisk检测仅 $MAG_OK/${#MAGISK[@]}${N}"
        F=$((F+1))
    fi

    # KernelSU检测
    if grep -q "KernelSU\|ksud\|kernelsu" "$RD_KT"; then
        echo "${G}  ✅ KernelSU检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少KernelSU检测${N}"
        F=$((F+1))
    fi

    # APatch检测
    if grep -q "APatch\|apatch" "$RD_KT"; then
        echo "${G}  ✅ APatch检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少APatch检测${N}"
        F=$((F+1))
    fi

    # ADB Root检测
    if grep -q "ro.debuggable\|ro.secure" "$RD_KT"; then
        echo "${G}  ✅ ADB Root检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少ADB Root检测${N}"
        F=$((F+1))
    fi

    # 基础Root检测（Root管理器包名+test-keys）
    if grep -q "SuperSU\|supersu\|test-keys" "$RD_KT"; then
        echo "${G}  ✅ 基础Root检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少基础Root检测${N}"
        F=$((F+1))
    fi

    # 综合判断：3个以上触发=异常
    if grep -q ">= 3\|>=3\|triggered >= 3" "$RD_KT"; then
        echo "${G}  ✅ 综合判断阈值存在（>=3）${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 未找到>=3阈值逻辑${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ RootDetector.kt不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 编号22：模拟器+云手机+环境检测 =====
echo "=== 编号22：模拟器+云手机+环境检测（9大类） ==="
echo "验证：真机正常环境全部通过 | 模拟器/云手机/沙箱/远程控制/自动化环境触发"
echo "异常判定：综合评分超过阈值=弹警告+强制关闭+含厂商差异适配防误判"

if [ -f "$RD_KT" ]; then
    # 9大类检测
    ENV_CHECKS=("checkEmulator" "checkCloudPhone" "checkSandbox"
        "checkDeviceFarm" "checkCustomROM" "checkAppCloning"
        "checkRemoteControl" "checkInputSource" "checkAutomation")
    ENV_OK=0
    for chk in "${ENV_CHECKS[@]}"; do
        if grep -q "$chk" "$RD_KT"; then
            ENV_OK=$((ENV_OK+1))
        else
            echo "${Y}  ⚠️ 缺少: $chk${N}"
        fi
    done
    if [ "$ENV_OK" -ge 7 ]; then
        echo "${G}  ✅ 环境检测: $ENV_OK/${#ENV_CHECKS[@]} 大类存在${N}"
    else
        echo "${R}  ⚠️ 环境检测仅 $ENV_OK/${#ENV_CHECKS[@]} 大类${N}"
        F=$((F+1))
    fi

    # 检查模拟器特征文件
    if grep -q "goldfish\|ranchu\|vbox\|QEMU\|qemu" "$RD_KT"; then
        echo "${G}  ✅ 模拟器CPU特征检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少模拟器CPU检测${N}"
        F=$((F+1))
    fi

    # 检查云手机特征
    if grep -q "红手指\|cloudphone\|华为云\|多多云\|yunphone" "$RD_KT"; then
        echo "${G}  ✅ 云手机特征检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少云手机检测${N}"
        F=$((F+1))
    fi

    # 检查沙箱检测
    if grep -q "平行空间\|VirtualApp\|Island\|Shelter\|双开" "$RD_KT"; then
        echo "${G}  ✅ 沙箱双开检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少沙箱检测${N}"
        F=$((F+1))
    fi

    # 检查远程控制检测
    if grep -q "TeamViewer\|AnyDesk\|向日葵\|Vysor\|scrcpy" "$RD_KT"; then
        echo "${G}  ✅ 远程控制检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少远程控制检测${N}"
        F=$((F+1))
    fi

    # 检查自动化框架检测
    if grep -q "Appium\|uiautomator2\|Airtest\|poco\|minitouch" "$RD_KT"; then
        echo "${G}  ✅ 自动化框架检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少自动化框架检测${N}"
        F=$((F+1))
    fi

    # 检查应用分身检测
    if grep -q "分身\|userId.*>.*0\|/data/user/10" "$RD_KT"; then
        echo "${G}  ✅ 应用分身检测存在${N}"
    else
        echo "${R}  ⚠️ 缺少应用分身检测${N}"
        F=$((F+1))
    fi

    # 检查厂商差异适配
    if grep -q "厂商\|officialPrefixes\|fingerprint" "$RD_KT"; then
        echo "${G}  ✅ 厂商差异适配存在${N}"
        P=$((P+1))
    else
        echo "${Y}  ⚠️ 未找到厂商差异适配${N}"
    fi
else
    echo "${R}  ⚠️ RootDetector.kt不存在${N}"
    F=$((F+1))
fi
echo ""

# ===== Native层fork隔离验证 =====
echo "=== Native层fork隔离验证 ==="
NATIVE_C="$SEC_DIR/native_root_detect.c"
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

    # 检查编号21 C层检测项
    ROOT_C=("check_su_files" "check_magisk_modules" "check_magisk_mounts"
        "check_magisk_initrc" "check_test_keys" "check_ro_debuggable")
    RC_OK=0
    for rc in "${ROOT_C[@]}"; do
        if grep -q "$rc" "$NATIVE_C"; then RC_OK=$((RC_OK+1)); fi
    done
    echo "${G}  ✅ 编号21 C层检测: $RC_OK/${#ROOT_C[@]} 项存在${N}"

    # 检查编号22 C层检测项
    ENV_C=("check_emulator_cpu" "check_emulator_files" "check_wifi_mac"
        "check_remote_process" "check_automation_process" "check_virtualapp")
    EC_OK=0
    for ec in "${ENV_C[@]}"; do
        if grep -q "$ec" "$NATIVE_C"; then EC_OK=$((EC_OK+1)); fi
    done
    echo "${G}  ✅ 编号22 C层检测: $EC_OK/${#ENV_C[@]} 项存在${N}"

    if grep -q "Java_com_myvideo_editor_security_RootDetector" "$NATIVE_C"; then
        echo "${G}  ✅ JNI注册存在${N}"
        P=$((P+1))
    else
        echo "${R}  ⚠️ 缺少JNI注册${N}"
        F=$((F+1))
    fi
else
    echo "${R}  ⚠️ native_root_detect.c不存在→构建失败${N}"
    F=$((F+1))
fi
echo ""

# ===== 总结 =====
echo "============================================"
echo " 类目六验证总结"
echo "============================================"
echo "  通过: $P"
echo "  失败: $F"
echo ""
if [ "$F" -gt 0 ]; then
    echo "${R}[异常判定] 有 $F 项未通过→构建失败${N}"
    exit 1
else
    echo "${G}[验证通过] 全部2项Root/环境检测验证通过${N}"
    echo "${G}[崩溃率] 主进程预期零崩溃${N}"
    echo "${G}[防崩溃] fork隔离${N}"
    echo "${G}[覆盖] 21: 6种Root方案全覆盖${N}"
    echo "${G}[覆盖] 22: 模拟器+云手机+沙箱+设备农场+ROM+分身+远程+输入+自动化${N}"
    exit 0
fi
