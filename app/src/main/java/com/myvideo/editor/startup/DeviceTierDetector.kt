package com.myvideo.editor.startup

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object DeviceTierDetector {

    enum class Tier(val label: String, val level: Int) {
        T3("入门机", 0),
        T2("中端机", 1),
        T1("旗舰机", 2)
    }

    data class DeviceInfo(
        val tier: Tier,
        val ramGb: Int,
        val cpuCores: Int,
        val cpuFreqMhz: Int,
        val apiLevel: Int,
        val availableStorageGb: Int
    )

    private var cached: DeviceInfo? = null

    fun detect(context: Context): DeviceInfo {
        cached?.let { return it }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramGb = (memInfo.totalMem / (1024L * 1024 * 1024)).toInt()
        val cores = Runtime.getRuntime().availableProcessors()
        val freq = getMaxCpuFreq()
        val api = Build.VERSION.SDK_INT
        val stat = android.os.StatFs(context.filesDir.path)
        val availGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024 * 1024)

        var score = 0
        score += when { ramGb >= 12 -> 30; ramGb >= 8 -> 25; ramGb >= 6 -> 15; else -> 5 }
        score += when { cores >= 8 -> 20; cores >= 6 -> 15; cores >= 4 -> 10; else -> 5 }
        score += when { freq >= 2800 -> 20; freq >= 2200 -> 15; freq >= 1800 -> 10; else -> 5 }
        score += when { api >= 33 -> 15; api >= 30 -> 10; api >= 28 -> 5; else -> 0 }
        score += when { availGb >= 10 -> 15; availGb >= 5 -> 10; availGb >= 3 -> 5; else -> 0 }

        val tier = when {
            score >= 75 -> Tier.T1
            score >= 45 -> Tier.T2
            else -> Tier.T3
        }

        val info = DeviceInfo(tier, ramGb, cores, freq, api, availGb.toInt())
        cached = info
        return info
    }

    private fun getMaxCpuFreq(): Int {
        return try {
            (0 until Runtime.getRuntime().availableProcessors()).maxOf { core ->
                java.io.File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")
                    .readText().trim().toIntOrNull() ?: 0
            }
        } catch (e: Exception) { 0 }
    }
}
