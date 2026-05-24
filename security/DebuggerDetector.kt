package com.myvideo.editor.security

import android.content.Context
import android.os.Debug
import android.provider.Settings

/**
 * NexClip 类目三：反调试
 * 编号18：多层调试器检测（8层交叉验证）
 * 编号33：混合层级检测（Java+C层交叉验证）
 *
 * 防崩溃方式：Java层标准API，C层fork隔离
 * 崩溃率：零（主进程）
 */
object DebuggerDetector {

    // ===== 编号18：多层调试器检测 =====
    // 做什么：8层检测交叉验证，任何调试器附加都能发现
    // 程度：任意2层以上触发=判定异常
    // 异常判定：2层以上触发=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    data class LayerResult(val name: String, val triggered: Boolean, val detail: String)

    /**
     * 层1：/proc/self/status中TracerPid!=0
     * 有调试器时TracerPid为调试器PID
     */
    private fun layer1_TracerPid(): LayerResult {
        return try {
            val status = java.io.File("/proc/self/status").readText()
            val match = Regex("TracerPid:\\s*(\\d+)").find(status)
            val pid = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            LayerResult("TracerPid", pid != 0, "TracerPid=$pid")
        } catch (e: Exception) {
            LayerResult("TracerPid", false, "读取异常")
        }
    }

    /**
     * 层2：/proc/self/wchan包含ptrace/traced
     * 被ptrace时wchan显示ptrace相关
     */
    private fun layer2_Wchan(): LayerResult {
        return try {
            val wchan = java.io.File("/proc/self/wchan").readText()
            val triggered = wchan.contains("ptrace") || wchan.contains("traced")
            LayerResult("Wchan", triggered, "wchan=$wchan")
        } catch (e: Exception) {
            LayerResult("Wchan", false, "读取异常")
        }
    }

    /**
     * 层3：JDWP端口检查
     * 有jdwp关键字=有调试器
     */
    private fun layer3_Jdwp(): LayerResult {
        return try {
            val cmdline = java.io.File("/proc/self/cmdline").readText()
            val triggered = cmdline.contains("jdwp")
            LayerResult("JDWP", triggered, "cmdline含jdwp=$triggered")
        } catch (e: Exception) {
            LayerResult("JDWP", false, "读取异常")
        }
    }

    /**
     * 层4：Debug.isDebuggerConnected()
     * Java标准API
     */
    private fun layer4_DebuggerConnected(): LayerResult {
        return try {
            val connected = Debug.isDebuggerConnected()
            LayerResult("DebuggerConnected", connected, "isDebuggerConnected=$connected")
        } catch (e: Exception) {
            LayerResult("DebuggerConnected", false, "API异常")
        }
    }

    /**
     * 层5：Debug.waitingForDebugger()
     */
    private fun layer5_WaitingDebugger(): LayerResult {
        return try {
            val waiting = Debug.waitingForDebugger()
            LayerResult("WaitingDebugger", waiting, "waitingForDebugger=$waiting")
        } catch (e: Exception) {
            LayerResult("WaitingDebugger", false, "API异常")
        }
    }

    /**
     * 层6：/proc/self/status中SigQ/SigPnd信号队列异常
     * 可能有调试器
     */
    private fun layer6_SignalQueue(): LayerResult {
        return try {
            val status = java.io.File("/proc/self/status").readText()
            val sigq = Regex("SigQ:\\s*(\\d+)/(\\d+)").find(status)
            val queued = sigq?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val triggered = queued > 0
            LayerResult("SignalQueue", triggered, "SigQ queued=$queued")
        } catch (e: Exception) {
            LayerResult("SignalQueue", false, "读取异常")
        }
    }

    /**
     * 层7：oom_score_adj异常降低
     * 调试器会保护被调试进程
     */
    private fun layer7_OomScore(): LayerResult {
        return try {
            val score = java.io.File("/proc/self/oom_score_adj").readText().trim().toIntOrNull() ?: 0
            val triggered = score < -900
            LayerResult("OomScore", triggered, "oom_score_adj=$score")
        } catch (e: Exception) {
            LayerResult("OomScore", false, "读取异常")
        }
    }

    /**
     * 层8：/proc/self/task中每个线程的TracerPid
     * 任何线程被trace=有调试器
     */
    private fun layer8_TaskTracer(): LayerResult {
        return try {
            val taskDir = java.io.File("/proc/self/task")
            val dirs = taskDir.listFiles() ?: emptyArray()
            for (tid in dirs) {
                val status = java.io.File(tid, "status")
                if (status.exists()) {
                    val content = status.readText()
                    val match = Regex("TracerPid:\\s*(\\d+)").find(content)
                    val pid = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    if (pid != 0) {
                        return LayerResult("TaskTracer", true, "tid=${tid.name} TracerPid=$pid")
                    }
                }
            }
            LayerResult("TaskTracer", false, "所有线程TracerPid=0")
        } catch (e: Exception) {
            LayerResult("TaskTracer", false, "读取异常")
        }
    }

    /**
     * 编号18：8层综合打分
     * 任意2层以上触发=判定异常
     */
    fun detectDebugger18(): Pair<Boolean, List<LayerResult>> {
        val layers = listOf(
            layer1_TracerPid(), layer2_Wchan(), layer3_Jdwp(),
            layer4_DebuggerConnected(), layer5_WaitingDebugger(),
            layer6_SignalQueue(), layer7_OomScore(), layer8_TaskTracer()
        )
        val triggered = layers.count { it.triggered }
        val abnormal = triggered >= 2
        return Pair(abnormal, layers)
    }

    // ===== 编号33：混合层级检测 =====
    // 做什么：Java层+C层多维度交叉检测
    // 程度：去掉不稳定的时序检测和ptrace自保护，两层结果交叉验证
    // 异常判定：任一层发现异常=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    /**
     * Java层检测（4项）
     * 1. Debug.isDebuggerConnected()
     * 2. Debug.waitingForDebugger()
     * 3. Settings.Global中adb_enabled
     * 4. 开发者选项是否开启
     */
    private fun javaLayerDetect(context: Context): Pair<Boolean, String> {
        return try {
            val results = mutableListOf<String>()
            var triggered = false

            // 1. Debug.isDebuggerConnected()
            val debuggerConnected = Debug.isDebuggerConnected()
            if (debuggerConnected) {
                results.add("debuggerConnected=true")
                triggered = true
            }

            // 2. Debug.waitingForDebugger()
            val waitingDebugger = Debug.waitingForDebugger()
            if (waitingDebugger) {
                results.add("waitingDebugger=true")
                triggered = true
            }

            // 3. adb_enabled
            val adbEnabled = try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
            } catch (e: Exception) { 0 }
            if (adbEnabled == 1) {
                results.add("adb_enabled=1")
                // adb开启不一定就是调试，单独不算触发
            }

            // 4. 开发者选项
            val devOptions = try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
            } catch (e: Exception) { 0 }
            if (devOptions == 1) {
                results.add("devOptions=1")
                // 开发者选项单独不算触发
            }

            Pair(triggered, results.joinToString(", "))
        } catch (e: Exception) {
            Pair(false, "Java检测异常")
        }
    }

    /**
     * C层fork隔离检测（3项）
     * 外部方法在native_anti_debug.c中实现
     * 1. /proc/self/fdinfo中调试器fd
     * 2. /proc/self/exe符号链接目标
     * 3. LD_PRELOAD环境变量检查
     */
    external fun nativeAntiDebugDetect(): Int

    /**
     * 编号33：Java层+C层交叉验证
     * 任何一层发现异常=弹警告
     */
    fun detectMixedLevel33(context: Context): Pair<Boolean, String> {
        return try {
            val (javaTriggered, javaDetail) = javaLayerDetect(context)

            val nativeResult = try {
                nativeAntiDebugDetect()
            } catch (e: Exception) {
                0
            }
            val nativeTriggered = nativeResult != 0

            val abnormal = javaTriggered || nativeTriggered
            val message = buildString {
                append("Java层: ${if (javaTriggered) "异常" else "正常"} ($javaDetail)")
                append(" | C层: ${if (nativeTriggered) "异常(code=$nativeResult)" else "正常"}")
                if (abnormal) append(" | 判定: 异常")
            }

            Pair(abnormal, message)
        } catch (e: Exception) {
            Pair(false, "混合检测异常: ${e.message}")
        }
    }

    // ===== 综合校验 =====

    data class AntiDebugResult(
        val passed: Boolean,
        val detect18Triggered: Boolean,
        val detect18Layers: List<LayerResult>,
        val detect33Triggered: Boolean,
        val detect33Detail: String,
        val message: String
    )

    /**
     * 完整反调试校验
     * 编号18 + 编号33 综合判断
     * 任一异常=弹警告+强制关闭
     */
    fun fullAntiDebugCheck(context: Context): AntiDebugResult {
        return try {
            val (d18Abnormal, d18Layers) = detectDebugger18()
            val (d33Abnormal, d33Detail) = detectMixedLevel33(context)

            val passed = !d18Abnormal && !d33Abnormal
            val message = buildString {
                append("编号18(8层): ${if (d18Abnormal) "异常" else "正常"}")
                append(" | 编号33(混合): ${if (d33Abnormal) "异常" else "正常"}")
                if (!passed) append(" | 判定: 检测到调试器")
            }

            if (!passed) {
                reportAnomaly(context, message)
            }

            AntiDebugResult(passed, d18Abnormal, d18Layers, d33Abnormal, d33Detail, message)
        } catch (e: Exception) {
            reportAnomaly(context, "反调试校验异常: ${e.message}")
            AntiDebugResult(true, false, emptyList(), false, "", "校验异常: ${e.message}")
        }
    }

    private fun reportAnomaly(context: Context, message: String) {
        try {
            SecurityReporter.report(context, "ANTI_DEBUG", message)
        } catch (e: Exception) { }
    }
}
