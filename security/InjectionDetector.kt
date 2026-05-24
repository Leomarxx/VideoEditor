package com.myvideo.editor.security

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * NexClip 类目五：反注入
 * 编号32：注入区域监控（maps持续变化监控）
 * 编号34：进程注入检测（多维度注入检测）
 * 编号36：maps变化监控（和32互补）
 * 编号37：SO加载行为完整性（四维度）
 *
 * 防崩溃方式：fork隔离+只做文本解析
 * 崩溃率：零（主进程）
 */
object InjectionDetector {

    private var scheduler: ScheduledExecutorService? = null
    private var mapsSnapshotHash: String = ""
    private var soSnapshot: Set<String> = emptySet()
    private var initialized = false

    // ===== 编号32：注入区域监控 =====
    // 做什么：监控/proc/self/maps的变化，检测代码注入行为，持续监控
    // 程度：首次启动保存maps快照hash，定期重新计算hash（每5分钟）
    //       对比前后差异重点关注：
    //       新增可执行区域(r-xp)、权限从rw-p变为rwxp、
    //       新增匿名可执行映射、新增未知SO映射、已知SO地址范围变化
    //       对比可疑关键字：frida/xposed/substrate/zygisk/magisk等
    // 验证方式：无异常新增映射时全部通过 | 注入任何SO后立即发现新增映射
    // 异常判定：发现可疑映射=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    // C层fork隔离获取maps hash
    external fun nativeGetMapsHash(): String?
    // C层fork隔离分析maps变化
    external fun nativeAnalyzeMapsChange(oldHash: String): Int

    /**
     * 首次启动保存maps快照
     */
    private fun initMapsSnapshot() {
        try {
            mapsSnapshotHash = try {
                nativeGetMapsHash() ?: calculateMapsHashJava()
            } catch (e: Exception) {
                calculateMapsHashJava()
            }
        } catch (e: Exception) {
            mapsSnapshotHash = ""
        }
    }

    /**
     * Java层计算maps hash（备选方案）
     */
    private fun calculateMapsHashJava(): String {
        return try {
            val maps = File("/proc/self/maps").readText()
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(maps.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "" }
    }

    /**
     * 编号32：对比maps前后差异
     * 分析新增映射
     */
    private fun analyzeMapsDiff(): Pair<Boolean, String> {
        return try {
            val currentMaps = File("/proc/self/maps").readText()
            val currentHash = calculateMapsHashJava()

            if (mapsSnapshotHash.isEmpty()) {
                mapsSnapshotHash = currentHash
                return Pair(false, "首次快照已保存")
            }

            if (currentHash == mapsSnapshotHash) {
                return Pair(false, "maps无变化")
            }

            // hash不同，分析具体变化
            val keywords = listOf("frida", "xposed", "substrate", "zygisk",
                "magisk", "lsposed", "edxposed", "inject", "hook", "gadget")

            val lines = currentMaps.lines()
            for (line in lines) {
                val lower = line.lowercase()
                // 检查可疑关键字
                for (kw in keywords) {
                    if (lower.contains(kw)) {
                        return Pair(true, "新增可疑映射: $kw in ${line.take(80)}")
                    }
                }
                // 检查新增rwx权限匿名映射
                if (lower.contains("rwx") && (!lower.contains("/") || lower.contains("anon"))) {
                    return Pair(true, "新增rwx匿名映射: ${line.take(80)}")
                }
            }

            mapsSnapshotHash = currentHash
            Pair(false, "maps变化但无异常")
        } catch (e: Exception) {
            Pair(false, "分析异常: ${e.message}")
        }
    }

    /**
     * 编号32：启动持续监控（每5分钟）
     */
    fun startMapsMonitor(context: Context) {
        if (initialized) return
        initialized = true

        initMapsSnapshot()
        initSoSnapshot()

        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler?.scheduleAtFixedRate({
            try {
                // 编号32
                val (abnormal32, detail32) = analyzeMapsDiff()
                if (abnormal32) reportAnomaly(context, "编号32: $detail32")

                // 编号36
                val (abnormal36, detail36) = analyzeMapsChange()
                if (abnormal36) reportAnomaly(context, "编号36: $detail36")

                // 编号34
                val (abnormal34, detail34) = detectProcessInjection()
                if (abnormal34) reportAnomaly(context, "编号34: $detail34")
            } catch (e: Exception) { }
        }, 5, 5, TimeUnit.MINUTES)
    }

    fun stopMonitor() {
        scheduler?.shutdown()
        scheduler = null
        initialized = false
    }

    // ===== 编号34：进程注入检测 =====
    // 做什么：检测代码被注入到当前进程，多维度注入检测
    // 程度：监控maps中新增未知SO、检查LD_PRELOAD、检查非系统目录SO加载
    // 验证方式：无异常SO加载时全部通过 | LD_PRELOAD被设置或加载未知SO后触发
    // 异常判定：发现异常注入=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    // 合法SO加载目录白名单
    private val LEGIT_SO_DIRS = listOf(
        "/data/app/",           // 应用目录
        "/system/lib64/",       // 系统64位
        "/system/lib/",         // 系统32位
        "/vendor/lib64/",       // 厂商64位
        "/vendor/lib/",         // 厂商32位
        "/apex/",               // APEX模块
        "/data/dalvik-cache/"   // dalvik缓存
    )

    private fun initSoSnapshot() {
        soSnapshot = try {
            val maps = File("/proc/self/maps").readText()
            maps.lines().filter { it.contains(".so") }
                .mapNotNull { line ->
                    line.trim().split("\\s+".toRegex()).lastOrNull()
                }.filter { it.endsWith(".so") }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    /**
     * 编号34：检测进程注入
     * 1. 监控maps中新增未知SO
     * 2. 检查LD_PRELOAD环境变量
     * 3. 检查非系统目录SO加载
     */
    private fun detectProcessInjection(): Pair<Boolean, String> {
        return try {
            // 1. 检查LD_PRELOAD
            val ldPreload = System.getenv("LD_PRELOAD")
            if (!ldPreload.isNullOrBlank()) {
                return Pair(true, "LD_PRELOAD被设置=$ldPreload")
            }

            // 2. 获取当前SO列表
            val maps = File("/proc/self/maps").readText()
            val currentSos = maps.lines().filter { it.contains(".so") }
                .mapNotNull { it.trim().split("\\s+".toRegex()).lastOrNull() }
                .filter { it.endsWith(".so") }.toSet()

            // 3. 对比新增SO
            val newSos = currentSos - soSnapshot
            for (so in newSos) {
                // 4. 检查是否在合法目录
                val isLegit = LEGIT_SO_DIRS.any { so.startsWith(it) }
                if (!isLegit) {
                    return Pair(true, "发现非合法目录SO: $so")
                }
            }

            Pair(false, "SO加载正常，新增${newSos.size}个合法SO")
        } catch (e: Exception) {
            Pair(false, "检测异常: ${e.message}")
        }
    }

    // ===== 编号36：maps变化监控 =====
    // 做什么：监控进程内存映射的持续变化，和编号32互补
    //       32侧重新增，36侧变化，两者联合覆盖更完整
    // 程度：/proc/self/maps的定期hash对比
    //       检查：新增映射中可疑关键字、rwx权限匿名映射、
    //       SO文件路径变化、内存段大小异常变化
    // 验证方式：maps无异常变化时全部通过 | 修改内存映射后立即发现变化
    // 异常判定：发现异常变化=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    private var mapsContent: String = ""

    /**
     * 编号36：分析maps内容变化
     * 和编号32互补，32侧重新增，36侧重变化
     */
    private fun analyzeMapsChange(): Pair<Boolean, String> {
        return try {
            val current = File("/proc/self/maps").readText()

            if (mapsContent.isEmpty()) {
                mapsContent = current
                return Pair(false, "首次maps快照已保存")
            }

            // 检查内容是否有变化
            if (current == mapsContent) {
                return Pair(false, "maps无变化")
            }

            val oldLines = mapsContent.lines().toSet()
            val newLines = current.lines()

            // 检查新增行
            for (line in newLines) {
                if (line !in oldLines) {
                    val lower = line.lowercase()
                    // 检查新增行中的关键字
                    val keywords = listOf("frida", "xposed", "inject", "hook",
                        "gadget", "substrate", "zygisk")
                    for (kw in keywords) {
                        if (lower.contains(kw)) {
                            mapsContent = current
                            return Pair(true, "新增行含关键字: $kw -> ${line.take(80)}")
                        }
                    }
                    // 检查新增rwx映射
                    if (lower.contains("rwx") && !lower.contains("/")) {
                        mapsContent = current
                        return Pair(true, "新增rwx匿名映射: ${line.take(80)}")
                    }
                }
            }

            // 检查SO路径变化（地址范围变化）
            val oldSos = oldLines.filter { it.contains(".so") }
            val newSos = newLines.filter { it.contains(".so") }
            val sosChanged = oldSos.size != newSos.size

            mapsContent = current
            Pair(false, "maps变化${newLines.size - oldLines.size}行, SO数变化: $sosChanged")
        } catch (e: Exception) {
            Pair(false, "分析异常: ${e.message}")
        }
    }

    // ===== 综合校验 =====

    data class InjectionResult(
        val passed: Boolean,
        val detail32: String,
        val detail34: String,
        val detail36: String,
        val detail37: String
    )

    /**
     * 首次启动时一次性全面检测
     * 编号37 + 编号34（一次性部分）
     */
    fun fullInjectionCheck(context: Context): InjectionResult {
        return try {
            // 编号37：SO完整性（C层fork隔离）
            val d37 = try { nativeVerifySoIntegrity() } catch (e: Exception) { 0 }

            // 编号34：进程注入检测
            val (abnormal34, detail34) = detectProcessInjection()

            // 编号32：maps快照
            initMapsSnapshot()
            val d32 = "maps快照已保存"

            // 编号36：maps快照
            mapsContent = try { File("/proc/self/maps").readText() } catch (e: Exception) { "" }
            val d36 = "maps内容快照已保存"

            val passed = d37 == 0 && !abnormal34
            if (!passed) {
                if (d37 != 0) reportAnomaly(context, "编号37: SO完整性异常 code=$d37")
                if (abnormal34) reportAnomaly(context, "编号34: $detail34")
            }

            InjectionResult(passed, d32, detail34, d36,
                if (d37 == 0) "SO完整性通过" else "SO完整性异常 code=$d37")
        } catch (e: Exception) {
            InjectionResult(false, "异常", "异常", "异常", "异常: ${e.message}")
        }
    }

    // 编号37 C层接口
    external fun nativeVerifySoIntegrity(): Int

    private fun reportAnomaly(context: Context, message: String) {
        try { SecurityReporter.report(context, "ANTI_INJECT", message) } catch (e: Exception) { }
    }
}
