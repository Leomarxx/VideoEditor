package com.myvideo.editor.engine

import android.content.Context
import com.myvideo.editor.startup.DeviceTierDetector
import com.myvideo.editor.startup.ModelConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * NexClip AI模型下载管理器
 * 根据设备档位自动选择对应精度的模型
 */
class ModelDownloader(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { if (!it.exists()) it.mkdirs() }

    var onProgress: (String, Float) -> Unit = { _, _ -> }
    var onComplete: (String, Boolean) -> Unit = { _, _ -> }
    var onStatusChange: (String) -> Unit = {}

    /**
     * 获取当前设备应下载的模型列表
     */
    fun getRequiredModels(): List<ModelConfig.ModelVariant> {
        val tier = DeviceTierDetector.detect(context).tier
        return ModelConfig.getModelSet(tier).models.values.toList()
    }

    fun isModelDownloaded(fileName: String): Boolean {
        return File(modelsDir, fileName).exists()
    }

    fun getModelPath(fileName: String): String? {
        val file = File(modelsDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    fun areAllModelsDownloaded(): Boolean {
        return getRequiredModels().all { isModelDownloaded(it.fileName) }
    }

    fun getDownloadedCount(): Int {
        return getRequiredModels().count { isModelDownloaded(it.fileName) }
    }

    fun getTotalCount(): Int = getRequiredModels().size

    /**
     * 下载单个模型
     */
    fun downloadModel(model: ModelConfig.ModelVariant) {
        Thread {
            try {
                val outputFile = File(modelsDir, model.fileName)
                if (outputFile.exists()) {
                    onComplete(model.fileName, true)
                    return@Thread
                }

                onStatusChange("正在下载: ${model.name} (${model.sizeMB}MB)")

                val url = URL(model.url)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 60000
                conn.readTimeout = 60000
                conn.connect()

                val totalSize = conn.contentLength.toLong()
                var downloadedSize = 0L

                conn.inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            val progress = if (totalSize > 0) downloadedSize.toFloat() / totalSize else 0f
                            onProgress(model.fileName, progress)
                        }
                    }
                }

                onComplete(model.fileName, true)
            } catch (e: Exception) {
                onComplete(model.fileName, false)
            }
        }.start()
    }

    /**
     * 下载当前设备所需的所有模型
     */
    fun downloadAllRequired() {
        val models = getRequiredModels()
        val totalMB = models.sumOf { it.sizeMB }
        onStatusChange("需下载 ${models.size} 个模型，共 ${totalMB}MB")

        Thread {
            models.forEach { model ->
                if (!isModelDownloaded(model.fileName)) {
                    downloadModel(model)
                    // 等待下载完成
                    Thread.sleep(1000)
                }
            }
        }.start()
    }

    fun deleteModel(fileName: String): Boolean {
        return File(modelsDir, fileName).delete()
    }

    fun deleteAll() {
        modelsDir.listFiles()?.forEach { it.delete() }
    }

    fun getTotalSizeMB(): Long {
        return (modelsDir.listFiles()?.sumOf { it.length() } ?: 0L) / (1024 * 1024)
    }

    /**
     * 获取设备信息摘要
     */
    fun getDeviceInfoSummary(): String {
        val info = DeviceTierDetector.detect(context)
        val models = getRequiredModels()
        val totalMB = models.sumOf { it.sizeMB }
        return buildString {
            appendLine("设备档位: ${info.tier.label}")
            appendLine("RAM: ${info.ramGb}GB | CPU: ${info.cpuCores}核 ${info.cpuFreqMhz}MHz")
            appendLine("可用存储: ${info.availableStorageGb}GB")
            appendLine("模型精度: ${if (models.any { it.isQuantized }) "INT8量化" else "FP16/FP32"}")
            appendLine("模型数量: ${models.size}个 | 总大小: ${totalMB}MB")
            appendLine("最高分辨率: ${ModelConfig.getMaxResolution(context)} @ ${ModelConfig.getMaxFps(context)}fps")
        }
    }
}
