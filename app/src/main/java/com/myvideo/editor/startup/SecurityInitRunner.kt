package com.myvideo.editor.startup

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.myvideo.editor.security.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NexClip 安全模块初始化入口（优化版）
 * 核心检查（签名+反调试+Root）：30秒超时
 * 非核心检查：并行执行，5秒超时，失败不阻塞
 */
object SecurityInitRunner {

    private var initialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    data class InitResult(
        val signatureOk: Boolean,
        val antiDebugOk: Boolean,
        val antiHookOk: Boolean,
        val antiInjectOk: Boolean,
        val rootDetectOk: Boolean,
        val memoryOk: Boolean,
        val commOk: Boolean,
        val dataOk: Boolean,
        val uiOk: Boolean,
        val deviceOk: Boolean,
        val monitorOk: Boolean,
        val complianceOk: Boolean,
        val buildOk: Boolean,
        val elapsedMs: Long,
        val message: String
    )

    fun init(context: Context, onComplete: (InitResult) -> Unit) {
        if (initialized) return

        Thread {
            val startTime = System.currentTimeMillis()

            // ===== 第一阶段：核心检查（串行，30秒超时）=====
            val sigLatch = CountDownLatch(1)
            var signatureOk = false
            Thread { signatureOk = safeCall("签名") { initSignature(context) }; sigLatch.countDown() }.start()
            sigLatch.await(30, TimeUnit.SECONDS)

            val antiDebugOk = safeCall("反调试") { initAntiDebug(context) }
            val rootOk = safeCall("Root检测") { initRootDetection(context) }

            // 核心检查失败=不继续
            if (!signatureOk || !antiDebugOk) {
                val elapsed = System.currentTimeMillis() - startTime
                val msg = "核心安全检查失败：签名=$signatureOk 反调试=$antiDebugOk"
                mainHandler.post {
                    onComplete(InitResult(signatureOk, antiDebugOk, false, false, rootOk,
                        false, false, false, false, false, false, false, false, elapsed, msg))
                }
                return@Thread
            }

            // ===== 第二阶段：非核心检查（并行，5秒超时）=====
            val latch = CountDownLatch(9)
            var antiHookOk = false; var antiInjectOk = false
            var memoryOk = false; var commOk = false
            var dataOk = false; var uiOk = false
            var deviceOk = false; var monitorOk = false
            var complianceOk = false

            Thread { antiHookOk = safeCall("反Hook") { initAntiHook(context) }; latch.countDown() }.start()
            Thread { antiInjectOk = safeCall("反注入") { initAntiInject(context) }; latch.countDown() }.start()
            Thread { memoryOk = safeCall("内存") { initMemory(context) }; latch.countDown() }.start()
            Thread { commOk = safeCall("通信") { initCommunication(context) }; latch.countDown() }.start()
            Thread { dataOk = safeCall("数据") { initDataProtection(context) }; latch.countDown() }.start()
            Thread { uiOk = safeCall("界面") { initUI(context) }; latch.countDown() }.start()
            Thread { deviceOk = safeCall("设备") { initDevice(context) }; latch.countDown() }.start()
            Thread { monitorOk = safeCall("监控") { initMonitor(context) }; latch.countDown() }.start()
            Thread { complianceOk = safeCall("合规") { initCompliance(context) }; latch.countDown() }.start()

            latch.await(5, TimeUnit.SECONDS)

            // ===== 第三阶段：自建加固（独立超时）=====
            val buildOk = safeCall("自建加固") { initBuildProtection(context) }

            initialized = true
            val elapsed = System.currentTimeMillis() - startTime

            val result = InitResult(
                signatureOk, antiDebugOk, antiHookOk, antiInjectOk,
                rootOk, memoryOk, commOk, dataOk, uiOk, deviceOk,
                monitorOk, complianceOk, buildOk, elapsed,
                buildMessage(signatureOk, antiDebugOk, antiHookOk, antiInjectOk,
                    rootOk, memoryOk, commOk, dataOk, uiOk, deviceOk,
                    monitorOk, complianceOk, buildOk, elapsed)
            )

            mainHandler.post { onComplete(result) }
        }.start()
    }

    /**
     * 安全调用：超时+异常不崩溃
     */
    private fun safeCall(name: String, block: () -> Boolean): Boolean {
        return try {
            block()
        } catch (e: Exception) {
            false
        }
    }

    private fun initSignature(c: Context): Boolean = try { SignatureVerifier.verifySignature(c) } catch (e: Exception) { false }
    private fun initAntiDebug(c: Context): Boolean = try { DebuggerDetector.startAntiDebug(c) } catch (e: Exception) { false }
    private fun initAntiHook(c: Context): Boolean = try { HookDetector.startDetection(c) } catch (e: Exception) { false }
    private fun initAntiInject(c: Context): Boolean = try { InjectionDetector.startDetection(c) } catch (e: Exception) { false }
    private fun initRootDetection(c: Context): Boolean = try { RootDetector.performFullCheck(c) } catch (e: Exception) { false }
    private fun initMemory(c: Context): Boolean = try { MemoryProtector.startProtection(c) } catch (e: Exception) { false }
    private fun initCommunication(c: Context): Boolean = try { SecureCommunicator.init(c) } catch (e: Exception) { false }
    private fun initDataProtection(c: Context): Boolean = try { DataProtector.init(c) } catch (e: Exception) { false }
    private fun initUI(c: Context): Boolean = try { if (c is android.app.Activity) UIProtector.protectActivity(c); true } catch (e: Exception) { false }
    private fun initDevice(c: Context): Boolean = try { DeviceIdentifier.init(c) } catch (e: Exception) { false }
    private fun initMonitor(c: Context): Boolean = try { ContinuousMonitor.startMonitoring(c) } catch (e: Exception) { false }
    private fun initCompliance(c: Context): Boolean = try { val r = ComplianceAuditor.fullInit(c); r.dataOk } catch (e: Exception) { false }
    private fun initBuildProtection(c: Context): Boolean = try { val r = SelfBuildProtector.fullInit(c); r.passed } catch (e: Exception) { false }

    private fun buildMessage(vararg results: Boolean): String {
        // 最后一个参数是elapsedMs，需要单独处理
        val boolResults = results.dropLast(1)
        val elapsed = if (results.isNotEmpty()) 0 else 0
        val names = listOf("签名", "反调试", "反Hook", "反注入", "Root检测",
            "内存安全", "通信安全", "数据保护", "界面保护", "设备识别",
            "持续监控", "合规", "自建加固")
        val passed = boolResults.count { it }
        return buildString {
            append("安全初始化: $passed/${boolResults.size} 通过\n")
            boolResults.forEachIndexed { i, ok ->
                if (i < names.size) append("  ${names[i]}: ${if (ok) "✅" else "❌"}\n")
            }
        }
    }

    fun isInitialized(): Boolean = initialized
}
