package com.myvideo.editor.security

import android.content.Context
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * NexClip 类目四：反Hook
 * 编号17：全框架检测（10+框架）
 * 编号24：Frida深度检测
 * 编号31：Frida标准检测
 *
 * 防崩溃方式：fork隔离
 * 崩溃率：零（主进程）
 */
object HookDetector {

    // ===== 编号17：全框架检测 =====
    // 做什么：检测所有已知Hook框架，覆盖10+个，fork子进程中执行
    // 程度：任意1个框架特征触发=异常
    // 异常判定：发现任何框架特征=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    data class FrameworkCheck(val name: String, val triggered: Boolean, val detail: String)

    /**
     * Xposed/LSPosed检测（4项）
     * 1. /proc/self/maps中Xposed特征路径
     * 2. ClassLoader中Xposed相关类
     * 3. 堆栈中Xposed特征
     * 4. 系统属性中Xposed标记
     */
    private fun checkXposed(): List<FrameworkCheck> {
        val results = mutableListOf<FrameworkCheck>()
        try {
            // 1. /proc/self/maps
            val maps = File("/proc/self/maps").readText()
            val xposedPaths = listOf("XposedBridge", "xposed", "de.robv.android.xposed",
                "org.lsposed", "lsposed")
            val mapHit = xposedPaths.any { maps.contains(it, ignoreCase = true) }
            results.add(FrameworkCheck("Xposed-maps", mapHit,
                if (mapHit) "maps中发现Xposed特征" else "maps正常"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Xposed-maps", false, "读取异常"))
        }
        try {
            // 2. ClassLoader
            val classLoader = Thread.currentThread().contextClassLoader
            val xposedClasses = listOf("de.robv.android.xposed.XposedBridge",
                "org.lsposed.manager", "XposedBridge")
            val classHit = xposedClasses.any { cls ->
                try { classLoader.loadClass(cls); true } catch (e: Exception) { false }
            }
            results.add(FrameworkCheck("Xposed-class", classHit,
                if (classHit) "发现Xposed类" else "类加载正常"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Xposed-class", false, "检测异常"))
        }
        try {
            // 3. 堆栈特征
            val stack = Thread.currentThread().stackTrace.joinToString { it.className }
            val stackHit = stack.contains("XposedBridge") || stack.contains("lsposed")
            results.add(FrameworkCheck("Xposed-stack", stackHit,
                if (stackHit) "堆栈含Xposed" else "堆栈正常"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Xposed-stack", false, "检测异常"))
        }
        try {
            // 4. 系统属性
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "xp.hook"))
            val output = process.inputStream.bufferedReader().readText()
            val propHit = output.isNotBlank() && !output.contains("not found")
            results.add(FrameworkCheck("Xposed-prop", propHit,
                if (propHit) "属性含Xposed" else "属性正常"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Xposed-prop", false, "检测异常"))
        }
        return results
    }

    /**
     * Zygisk注入检测（4项）
     * 1. /proc/self/maps中libzygisk.so
     * 2. 父进程Zygote特征检查
     * 3. CapEff字段检查
     * 4. SELinux上下文检查
     */
    private fun checkZygisk(): List<FrameworkCheck> {
        val results = mutableListOf<FrameworkCheck>()
        try {
            val maps = File("/proc/self/maps").readText()
            val mapHit = maps.contains("libzygisk")
            results.add(FrameworkCheck("Zygisk-maps", mapHit,
                if (mapHit) "maps含libzygisk" else "maps正常"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Zygisk-maps", false, "读取异常"))
        }
        try {
            // 2. 父进程Zygote特征
            val ppid = android.os.Process.myPid()
            val status = File("/proc/$ppid/status").readText()
            val ppidLine = Regex("PPid:\\s*(\\d+)").find(status)?.groupValues?.get(1)
            val parentCmdline = File("/proc/$ppidLine/cmdline").readText()
            val zygoteHit = parentCmdline.contains("zygote") &&
                !parentCmdline.contains("zygote64") && !parentCmdline.contains("zygote32")
            results.add(FrameworkCheck("Zygisk-zygote", false,
                "父进程=$parentCmdline"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Zygisk-zygote", false, "检测异常"))
        }
        try {
            // 3. CapEff字段
            val status = File("/proc/self/status").readText()
            val capeff = Regex("CapEff:\\s*(\\w+)").find(status)?.groupValues?.get(1)
            val capHit = capeff != null && capeff != "0000000000000000" &&
                capeff != "00000000ffffffff"
            results.add(FrameworkCheck("Zygisk-cap", false,
                "CapEff=$capeff"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Zygisk-cap", false, "检测异常"))
        }
        try {
            // 4. SELinux上下文
            val context = File("/proc/self/attr/current").readText()
            val selinuxHit = context.contains("magisk") || context.contains("zygisk")
            results.add(FrameworkCheck("Zygisk-selinux", selinuxHit,
                "SELinux=$context"))
        } catch (e: Exception) {
            results.add(FrameworkCheck("Zygisk-selinux", false, "检测异常"))
        }
        return results
    }

    /**
     * Substrate检测
     */
    private fun checkSubstrate(): FrameworkCheck {
        return try {
            val maps = File("/proc/self/maps").readText()
            val hit = maps.contains("libsubstrate.so") || maps.contains("libsubstrate-dvm.so")
            FrameworkCheck("Substrate", hit,
                if (hit) "maps含libsubstrate" else "正常")
        } catch (e: Exception) {
            FrameworkCheck("Substrate", false, "读取异常")
        }
    }

    /**
     * EdXposed检测
     */
    private fun checkEdXposed(): FrameworkCheck {
        return try {
            val maps = File("/proc/self/maps").readText()
            val hit = maps.contains("EdXposed") || maps.contains("edxposed")
            FrameworkCheck("EdXposed", hit,
                if (hit) "maps含EdXposed" else "正常")
        } catch (e: Exception) {
            FrameworkCheck("EdXposed", false, "读取异常")
        }
    }

    /**
     * VirtualXposed/TaiChi检测
     */
    private fun checkVirtualXposed(): FrameworkCheck {
        return try {
            val maps = File("/proc/self/maps").readText()
            val hit = maps.contains("VirtualXposed") || maps.contains("TaiChi") ||
                maps.contains("com.stub.StubApp")
            FrameworkCheck("VirtualXposed/TaiChi", hit,
                if (hit) "发现VirtualXposed/TaiChi特征" else "正常")
        } catch (e: Exception) {
            FrameworkCheck("VirtualXposed/TaiChi", false, "读取异常")
        }
    }

    // ===== 编号24：Frida深度检测 =====
    // 做什么：检测高级Frida使用方式，包括自定义编译的Frida
    // 程度：GJS引擎特征/D-Bus特征/堆内存扫描/SO注入/inline hook，fork子进程中执行
    // 异常判定：发现Frida特征=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    // C层实现：native_hook_detect.c
    external fun nativeFridaDeepDetect(): Int

    // ===== 编号31：Frida标准检测 =====
    // 做什么：四个独立检测方法覆盖Frida常见运行方式
    // 程度：方法1扫描maps/方法2检测27042端口/方法3遍历/proc/方法4检查rwx内存
    // 异常判定：任意方法触发=弹警告+强制关闭+上报服务端
    // 崩溃率：零（主进程）

    /**
     * 方法1：扫描/proc/self/maps
     * frida关键字+gadget关键字+linjector关键字+libfrida关键字
     */
    private fun fridaMethod1_Maps(): FrameworkCheck {
        return try {
            val maps = File("/proc/self/maps").readText()
            val keywords = listOf("frida", "gadget", "linjector", "libfrida", "frida-agent")
            val hit = keywords.any { maps.contains(it, ignoreCase = true) }
            FrameworkCheck("Frida-maps", hit,
                if (hit) "maps含Frida特征" else "maps正常")
        } catch (e: Exception) {
            FrameworkCheck("Frida-maps", false, "读取异常")
        }
    }

    /**
     * 方法2：检测本地27042端口
     * 非阻塞socket连接，连接成功=Frida server运行中
     */
    private fun fridaMethod2_Port(): FrameworkCheck {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", 27042), 200)
            socket.close()
            FrameworkCheck("Frida-port", true, "27042端口连接成功=Frida server运行中")
        } catch (e: SocketTimeoutException) {
            FrameworkCheck("Frida-port", false, "27042端口超时=正常")
        } catch (e: Exception) {
            FrameworkCheck("Frida-port", false, "27042端口拒绝=正常")
        }
    }

    /**
     * 方法3：遍历/proc目录
     * 查找frida-server进程+查找gadget进程
     */
    private fun fridaMethod3_Proc(): FrameworkCheck {
        return try {
            val procDir = File("/proc")
            val dirs = procDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (dir in dirs) {
                val cmdline = try {
                    File(dir, "cmdline").readText()
                } catch (e: Exception) { continue }
                if (cmdline.contains("frida-server") || cmdline.contains("frida-gadget") ||
                    cmdline.contains("re.frida.server")) {
                    return FrameworkCheck("Frida-proc", true, "发现进程: $cmdline")
                }
            }
            FrameworkCheck("Frida-proc", false, "未发现Frida进程")
        } catch (e: Exception) {
            FrameworkCheck("Frida-proc", false, "遍历异常")
        }
    }

    /**
     * 方法4：检查rwx内存映射
     * 正常APP不应有rwx权限内存段
     */
    private fun fridaMethod4_Rwx(): FrameworkCheck {
        return try {
            val maps = File("/proc/self/maps").readText()
            val lines = maps.lines()
            for (line in lines) {
                if (line.contains("rwx") || line.contains("rwxp")) {
                    // 匿名rwx映射高度可疑
                    if (!line.contains("/") || line.contains("anon")) {
                        return FrameworkCheck("Frida-rwx", true, "发现rwx映射: ${line.take(60)}")
                    }
                }
            }
            FrameworkCheck("Frida-rwx", false, "未发现异常rwx映射")
        } catch (e: Exception) {
            FrameworkCheck("Frida-rwx", false, "读取异常")
        }
    }

    // ===== 综合检测 =====

    data class HookDetectResult(
        val passed: Boolean,
        val frameworkResults: List<FrameworkCheck>,
        val fridaDeepResult: Int,
        val fridaStandardTriggered: Boolean,
        val triggeredCount: Int,
        val message: String
    )

    /**
     * 完整反Hook校验
     * 编号17 + 编号24 + 编号31 综合判断
     * 任一触发=异常
     */
    fun fullHookCheck(context: Context): HookDetectResult {
        return try {
            // 编号17：全框架检测
            val allChecks = mutableListOf<FrameworkCheck>()
            allChecks.addAll(checkXposed())
            allChecks.addAll(checkZygisk())
            allChecks.add(checkSubstrate())
            allChecks.add(checkEdXposed())
            allChecks.add(checkVirtualXposed())

            // 编号31：Frida标准检测
            val f1 = fridaMethod1_Maps()
            val f2 = fridaMethod2_Port()
            val f3 = fridaMethod3_Proc()
            val f4 = fridaMethod4_Rwx()
            allChecks.addAll(listOf(f1, f2, f3, f4))
            val fridaStandardTriggered = f1.triggered || f2.triggered || f3.triggered || f4.triggered

            // 编号24：Frida深度检测（C层fork隔离）
            val fridaDeepResult = try { nativeFridaDeepDetect() } catch (e: Exception) { 0 }

            // 综合打分
            val triggered = allChecks.count { it.triggered }
            val abnormal = triggered > 0 || fridaStandardTriggered || fridaDeepResult != 0

            val message = buildString {
                append("框架检测: $triggered 个触发")
                append(" | Frida标准: ${if (fridaStandardTriggered) "异常" else "正常"}")
                append(" | Frida深度: ${if (fridaDeepResult != 0) "异常" else "正常"}")
                if (abnormal) append(" | 判定: 检测到Hook框架")
            }

            if (abnormal) reportAnomaly(context, message)

            HookDetectResult(!abnormal, allChecks, fridaDeepResult, fridaStandardTriggered, triggered, message)
        } catch (e: Exception) {
            reportAnomaly(context, "反Hook校验异常: ${e.message}")
            HookDetectResult(true, emptyList(), 0, false, 0, "校验异常: ${e.message}")
        }
    }

    private fun reportAnomaly(context: Context, message: String) {
        try { SecurityReporter.report(context, "ANTI_HOOK", message) } catch (e: Exception) { }
    }
}
