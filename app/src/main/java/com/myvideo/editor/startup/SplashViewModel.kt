package com.myvideo.editor.startup

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myvideo.editor.engine.ModelDownloader

class SplashViewModel {
    var isLoading by mutableStateOf(true)
    var loadMessage by mutableStateOf("正在初始化...")
    var deviceInfo by mutableStateOf("")
    var modelStatus by mutableStateOf("")
    var isReady by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)

    fun startLoading(context: Context) {
        Thread {
            loadMessage = "检测设备性能..."
            Thread.sleep(300)

            val info = DeviceTierDetector.detect(context)
            deviceInfo = "${info.tier.label} · ${info.ramGb}GB · ${info.cpuCores}核"

            loadMessage = "配置AI模型..."
            val downloader = ModelDownloader(context)
            val models = downloader.getRequiredModels()
            val totalMB = models.sumOf { it.sizeMB }
            val downloaded = downloader.getDownloadedCount()
            modelStatus = "${models.firstOrNull()?.description?.split("，")?.firstOrNull() ?: ""} · $downloaded/${models.size}已下载 · 共${totalMB}MB"

            loadMessage = "加载资源..."
            Thread.sleep(300)

            loadMessage = "准备就绪"
            isLoading = false
            isReady = true
        }.start()
    }
}
