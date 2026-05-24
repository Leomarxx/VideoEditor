package com.myvideo.editor.security

import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.security.MessageDigest

object DeviceIdentifier {
    private var cachedFingerprint: String = ""
    private var fingerprintComponents: Map<String, String> = emptyMap()
    private var touchSamples = mutableListOf<TouchSample>()
    private var sensorData = mutableListOf<FloatArray>()
    private var reactionTimes = mutableListOf<Long>()
    private var boundDeviceId: String = ""
    private val MAX_FREE_DEVICES = 1
    private val MAX_VIP_DEVICES = 3

    data class TouchSample(
        val pressure: Float, val size: Float,
        val x: Float, val y: Float, val timestamp: Long
    )

    external fun nativeGetCpuFingerprint(): String?
    external fun nativeGetGpuRenderer(): String?
    external fun nativeGetKernelVersion(): String?
    external fun nativeGetInstalledAppsHash(): String?
    external fun nativeGetFontListHash(): String?

    private fun collectHardwareFingerprint(context: Context): Map<String, String> {
        val c = mutableMapOf<String, String>()
        try {
            c["cpu_cores"] = Runtime.getRuntime().availableProcessors().toString()
            try {
                val info = File("/proc/cpuinfo").readText()
                Regex("cpu MHz\\s*:\\s*(\\d+)").find(info)?.let { c["cpu_freq"] = it.groupValues[1] }
                Regex("Hardware\\s*:\\s*(.+)").find(info)?.let { c["cpu_arch"] = it.groupValues[1].trim() }
            } catch (e: Exception) { }
            try { c["cpu_native"] = nativeGetCpuFingerprint() ?: "unknown" } catch (e: Exception) { }
            try { c["gpu"] = GLES20.glGetString(GLES20.GL_RENDERER) ?: "unknown" } catch (e: Exception) { }
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val mi = android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
                c["ram"] = mi.totalMem.toString()
            } catch (e: Exception) { }
            try {
                val s = android.os.StatFs(context.filesDir.path)
                c["storage"] = (s.blockCountLong * s.blockSizeLong).toString()
            } catch (e: Exception) { }
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val dm = DisplayMetrics(); wm.defaultDisplay.getRealMetrics(dm)
                c["screen"] = "${dm.widthPixels}x${dm.heightPixels}_${dm.densityDpi}dpi"
            } catch (e: Exception) { }
            c["manufacturer"] = Build.MANUFACTURER ?: ""; c["brand"] = Build.BRAND ?: ""
            c["model"] = Build.MODEL ?: ""; c["device"] = Build.DEVICE ?: ""
            c["product"] = Build.PRODUCT ?: ""; c["board"] = Build.BOARD ?: ""
        } catch (e: Exception) { }
        return c
    }

    private fun collectSystemFingerprint(): Map<String, String> {
        val c = mutableMapOf<String, String>()
        try {
            c["os_version"] = Build.VERSION.RELEASE ?: ""; c["sdk_int"] = Build.VERSION.SDK_INT.toString()
            c["build_id"] = Build.ID ?: ""; c["build_fingerprint"] = Build.FINGERPRINT ?: ""
            try { c["kernel"] = nativeGetKernelVersion() ?: File("/proc/version").readText().take(80) } catch (e: Exception) { }
            try { c["apps_hash"] = nativeGetInstalledAppsHash() ?: "unknown" } catch (e: Exception) { }
            try { c["fonts_hash"] = nativeGetFontListHash() ?: "unknown" } catch (e: Exception) { }
        } catch (e: Exception) { }
        return c
    }

    fun recordTouchSample(pressure: Float, size: Float, x: Float, y: Float) {
        touchSamples.add(TouchSample(pressure, size, x, y, System.currentTimeMillis()))
        if (touchSamples.size > 100) touchSamples = touchSamples.takeLast(50).toMutableList()
    }

    fun recordReactionTime(startTime: Long) {
        reactionTimes.add(System.currentTimeMillis() - startTime)
        if (reactionTimes.size > 50) reactionTimes = reactionTimes.takeLast(25).toMutableList()
    }

    fun recordSensorData(values: FloatArray) {
        sensorData.add(values.copyOf())
        if (sensorData.size > 50) sensorData = sensorData.takeLast(25).toMutableList()
    }

    private fun computeBehaviorFingerprint(): Map<String, String> {
        val c = mutableMapOf<String, String>()
        try {
            if (touchSamples.isNotEmpty()) {
                c["touch_pressure"] = String.format("%.4f", touchSamples.map { it.pressure }.average())
                c["touch_size"] = String.format("%.4f", touchSamples.map { it.size }.average())
                if (touchSamples.size >= 3) {
                    var curv = 0.0
                    for (i in 1 until touchSamples.size - 1) {
                        val (p0, p1, p2) = Triple(touchSamples[i-1], touchSamples[i], touchSamples[i+1])
                        val cross = (p1.x-p0.x)*(p2.y-p1.y) - (p1.y-p0.y)*(p2.x-p1.x)
                        val dot = (p1.x-p0.x)*(p2.x-p1.x) + (p1.y-p0.y)*(p2.y-p1.y)
                        curv += kotlin.math.abs(kotlin.math.atan2(cross.toDouble(), dot.toDouble()))
                    }
                    c["touch_curvature"] = String.format("%.4f", curv / (touchSamples.size - 2))
                }
            }
            if (reactionTimes.isNotEmpty()) c["reaction_time"] = String.format("%.0f", reactionTimes.average())
            if (sensorData.isNotEmpty()) {
                c["sensor_x"] = String.format("%.4f", sensorData.map { it.getOrElse(0){0f} }.average())
                c["sensor_y"] = String.format("%.4f", sensorData.map { it.getOrElse(1){0f} }.average())
                c["sensor_z"] = String.format("%.4f", sensorData.map { it.getOrElse(2){0f} }.average())
            }
        } catch (e: Exception) { }
        return c
    }

    fun generateDeviceFingerprint(context: Context): String {
        return try {
            val all = mutableMapOf<String, String>()
            all.putAll(collectHardwareFingerprint(context))
            all.putAll(collectSystemFingerprint())
            all.putAll(computeBehaviorFingerprint())
            val combined = all.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }
            val hash = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
                .joinToString("") { "%02x".format(it) }
            cachedFingerprint = hash; fingerprintComponents = all; hash
        } catch (e: Exception) { "error_fingerprint" }
    }

    fun getCachedFingerprint(): String = cachedFingerprint

    fun verifyFingerprintConsistency(context: Context): Pair<Boolean, String> {
        return try {
            val current = generateDeviceFingerprint(context)
            if (cachedFingerprint.isEmpty()) { cachedFingerprint = current; return Pair(true, "首次生成") }
            if (current == cachedFingerprint) return Pair(true, "指纹一致")
            val hw = collectHardwareFingerprint(context)
            val sys = collectSystemFingerprint()
            val cur = mutableMapOf<String, String>(); cur.putAll(hw); cur.putAll(sys)
            val changes = cur.filter { (k, v) -> fingerprintComponents[k] != null && fingerprintComponents[k] != v }.keys
            Pair(false, "指纹变化: ${changes.joinToString(", ")}")
        } catch (e: Exception) { Pair(true, "验证异常: ${e.message}") }
    }

    fun checkDeviceLimit(isVip: Boolean, currentCount: Int): Pair<Boolean, String> {
        val max = if (isVip) MAX_VIP_DEVICES else MAX_FREE_DEVICES
        return if (currentCount < max) Pair(true, "设备正常: $currentCount/$max")
        else Pair(false, "设备超限: $currentCount/$max, 需踢掉最早设备")
    }

    fun bindDevice(context: Context, licenseId: String): String {
        val id = generateDeviceFingerprint(context); boundDeviceId = id; return id
    }

    fun verifyDeviceBinding(expectedDeviceId: String): Boolean {
        return try { boundDeviceId == expectedDeviceId } catch (e: Exception) { false }
    }

    fun getDeviceInfoSummary(context: Context): Map<String, String> {
        val hw = collectHardwareFingerprint(context)
        val sys = collectSystemFingerprint()
        return mapOf(
            "manufacturer" to (hw["manufacturer"] ?: ""), "model" to (hw["model"] ?: ""),
            "os_version" to (sys["os_version"] ?: ""), "sdk_int" to (sys["sdk_int"] ?: ""),
            "cpu_cores" to (hw["cpu_cores"] ?: ""), "ram" to (hw["ram"] ?: ""),
            "screen" to (hw["screen"] ?: ""), "fingerprint" to cachedFingerprint
        )
    }

    fun clearBehaviorData() { touchSamples.clear(); sensorData.clear(); reactionTimes.clear() }

    private fun reportAnomaly(context: Context, message: String) {
        try { SecurityReporter.report(context, "DEVICE", message) } catch (e: Exception) { }
    }
}
