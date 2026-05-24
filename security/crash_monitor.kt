package com.myvideo.editor.security

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * NexClip 构建期防护 - 崩溃率监控
 * 覆盖编号1/2/8/9/28/29全部6个的崩溃率监控
 * 崩溃率目标：零
 *
 * 功能：
 * 1. 全局崩溃捕获（UncaughtExceptionHandler）
 * 2. 崩溃日志记录（本地存储）
 * 3. 崩溃分类（标记是哪个安全模块导致）
 * 4. 上报服务端（可选）
 * 5. 崩溃率统计
 */
object CrashMonitor {

    private const val CRASH_DIR = "crash_logs"
    private const val TAG = "CrashMonitor"

    // 安全模块标识
    object Module {
        const val R8混淆 = "R8混淆"
        const val 字符串加密 = "字符串加密"
        const val 符号表Strip = "符号表Strip"
        const val 日志清除 = "日志清除"
        const val 控制流平坦化 = "控制流平坦化"
        const val 资源混淆 = "资源混淆"
    }

    // 崩溃记录
    data class CrashRecord(
        val timestamp: Long = System.currentTimeMillis(),
        val module: String = "未知",
        val exception: String = "",
        val stackTrace: String = "",
        val deviceInfo: String = "",
        val appVersion: String = ""
    )

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashRecords = mutableListOf<CrashRecord>()
    private var totalLaunches = 0
    private var totalCrashes = 0

    /**
     * 初始化崩溃监控
     * 在Application.onCreate中调用
     */
    fun init(context: Context) {
        totalLaunches = getLaunchCount(context)
        saveLaunchCount(context, totalLaunches + 1)

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val record = CrashRecord(
                module = identifyModule(throwable),
                exception = throwable.message ?: "未知异常",
                stackTrace = throwable.stackTraceToString(),
                deviceInfo = getDeviceInfo(),
                appVersion = getAppVersion(context)
            )

            // 保存崩溃记录
            saveCrashRecord(context, record)

            // 更新统计
            totalCrashes++
            saveCrashCount(context, totalCrashes)

            // 交给默认处理器（系统崩溃弹窗）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 识别崩溃属于哪个安全模块
     */
    private fun identifyModule(throwable: Throwable): String {
        val stack = throwable.stackTraceToString()
        val msg = throwable.message ?: ""

        return when {
            // 编号1：R8混淆导致的类找不到/方法找不到
            stack.contains("ClassNotFoundException") ||
            stack.contains("NoSuchMethodError") ||
            stack.contains("NoSuchFieldError") -> Module.R8混淆

            // 编号2：字符串解密失败
            stack.contains("nexclip_string_enc") ||
            stack.contains("xor_decrypt") ||
            stack.contains("decrypt_string") ||
            msg.contains("decrypt") -> Module.字符串加密

            // 编号8：SO符号找不到（strip过度）
            stack.contains("UnsatisfiedLinkError") ||
            stack.contains("dlopen failed") ||
            stack.contains("JNI_ERR") -> Module.符号表Strip

            // 编号9：日志相关（理论上不会，以防万一）
            msg.contains("Timber") ||
            msg.contains("Logger") -> Module.日志清除

            // 编号28：OLLVM平坦化导致的性能问题或崩溃
            stack.contains("security_static") ||
            stack.contains("detect_") ||
            stack.contains("crypto_") ||
            stack.contains("verify_") -> Module.控制流平坦化

            // 编号29：资源找不到（资源混淆过度）
            stack.contains("Resources\$NotFoundException") ||
            stack.contains("InflateException") ||
            stack.contains("NoSuchResourceError") -> Module.资源混淆

            else -> "未知模块"
        }
    }

    /**
     * 保存崩溃记录到本地
     */
    private fun saveCrashRecord(context: Context, record: CrashRecord) {
        try {
            val dir = File(context.filesDir, CRASH_DIR)
            if (!dir.exists()) dir.mkdirs()

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "crash_${sdf.format(Date(record.timestamp))}.log"
            val file = File(dir, filename)

            val content = buildString {
                appendLine("=== NexClip Crash Report ===")
                appendLine("时间: ${sdf.format(Date(record.timestamp))}")
                appendLine("模块: ${record.module}")
                appendLine("异常: ${record.exception}")
                appendLine("设备: ${record.deviceInfo}")
                appendLine("版本: ${record.appVersion}")
                appendLine("=== 堆栈 ===")
                appendLine(record.stackTrace)
            }

            file.writeText(content)
        } catch (e: Exception) {
            // 崩溃记录保存失败不能二次崩溃
        }
    }

    /**
     * 获取崩溃率统计
     */
    fun getCrashRate(context: Context): CrashStats {
        val launches = getLaunchCount(context)
        val crashes = getCrashCount(context)
        return CrashStats(
            totalLaunches = launches,
            totalCrashes = crashes,
            crashRate = if (launches > 0) crashes.toFloat() / launches else 0f,
            targetRate = 0f // 目标崩溃率：零
        )
    }

    /**
     * 获取模块级崩溃统计
     */
    fun getModuleCrashStats(context: Context): Map<String, Int> {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) return emptyMap()

        val stats = mutableMapOf<String, Int>()
        dir.listFiles()?.forEach { file ->
            try {
                val content = file.readText()
                val moduleMatch = Regex("模块: (.+)").find(content)
                val module = moduleMatch?.groupValues?.get(1) ?: "未知"
                stats[module] = (stats[module] ?: 0) + 1
            } catch (e: Exception) { }
        }
        return stats
    }

    /**
     * 清理旧崩溃日志
     */
    fun cleanOldLogs(context: Context, keepDays: Int = 7) {
        val dir = File(context.filesDir, CRASH_DIR) ?: return
        if (!dir.exists()) return
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }

    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} " +
                "Android${Build.VERSION.SDK_INT} " +
                "${Build.CPU_ABI}"
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName}(${pi.versionCode})"
        } catch (e: Exception) { "未知" }
    }

    private fun getLaunchCount(context: Context): Int {
        return context.getSharedPreferences("crash_monitor", Context.MODE_PRIVATE)
            .getInt("launch_count", 0)
    }

    private fun saveLaunchCount(context: Context, count: Int) {
        context.getSharedPreferences("crash_monitor", Context.MODE_PRIVATE)
            .edit().putInt("launch_count", count).apply()
    }

    private fun getCrashCount(context: Context): Int {
        return context.getSharedPreferences("crash_monitor", Context.MODE_PRIVATE)
            .getInt("crash_count", 0)
    }

    private fun saveCrashCount(context: Context, count: Int) {
        context.getSharedPreferences("crash_monitor", Context.MODE_PRIVATE)
            .edit().putInt("crash_count", count).apply()
    }

    data class CrashStats(
        val totalLaunches: Int,
        val totalCrashes: Int,
        val crashRate: Float,
        val targetRate: Float
    ) {
        fun isTargetMet(): Boolean = crashRate <= targetRate
        fun report(): String {
            return "启动次数:$totalLaunches 崩溃次数:$totalCrashes " +
                    "崩溃率:${String.format("%.4f", crashRate * 100)}% " +
                    "目标:${String.format("%.0f", targetRate * 100)}% " +
                    "状态:${if (isTargetMet()) "✅达标" else "⚠️未达标"}"
        }
    }
}
