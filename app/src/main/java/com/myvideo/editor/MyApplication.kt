package com.myvideo.editor

import android.app.Application
import android.util.Log
import com.myvideo.editor.security.SecurityReporter
import com.myvideo.editor.startup.SecurityInitRunner

/**
 * NexClip Application
 * 安全模块初始化入口
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化安全模块（14个类目）
        SecurityInitRunner.init(this) { result ->
            if (result.signatureOk && result.antiDebugOk) {
                // 核心安全检查通过，APP正常运行
                SecurityReporter.report(this, "APP_INIT", result.message, "INFO")
            } else {
                // 核心安全检查失败
                SecurityReporter.report(this, "APP_INIT", result.message, "CRITICAL")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            com.myvideo.editor.security.SelfBuildProtector.cleanup()
            com.myvideo.editor.security.ContinuousMonitor.cleanup()
            com.myvideo.editor.security.ComplianceAuditor.cleanup()
            com.myvideo.editor.security.SecurityReporter.clearEvents()
        } catch (e: Exception) { }
    }
}
