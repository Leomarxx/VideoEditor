package com.myvideo.editor.security

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * NexClip 安全事件上报器
 * 被全部14个类目引用
 * 本地缓存+服务端上报（服务端就绪后启用）
 */
object SecurityReporter {

    private val eventQueue = ConcurrentLinkedQueue<SecurityEvent>()
    private var initialized = false

    data class SecurityEvent(
        val category: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val severity: String = "WARN"
    )

    fun report(context: Context, category: String, message: String, severity: String = "WARN") {
        try {
            val event = SecurityEvent(
                category = ComplianceAuditor.sanitizeLogData(category),
                message = ComplianceAuditor.sanitizeLogData(message),
                severity = severity
            )
            eventQueue.add(event)

            // 写入本地日志文件
            writeToFile(context, event)

            // 上报服务端（暂用本地队列，服务端就绪后启用）
            ContinuousMonitor.reportBehavior(context, "security_event",
                mapOf("category" to event.category,
                      "message" to event.message,
                      "severity" to event.severity))
        } catch (e: Exception) { }
    }

    private fun writeToFile(context: Context, event: SecurityEvent) {
        try {
            val logDir = File(context.filesDir, "security_logs")
            if (!logDir.exists()) logDir.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.timestamp))
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(event.timestamp))
            val logFile = File(logDir, "security_$date.log")

            val line = "[$time] [${event.severity}] [${event.category}] ${event.message}\n"
            logFile.appendText(line)
        } catch (e: Exception) { }
    }

    fun getPendingEvents(): List<SecurityEvent> {
        val events = mutableListOf<SecurityEvent>()
        while (eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { events.add(it) }
        }
        return events
    }

    fun clearEvents() {
        eventQueue.clear()
    }
}
