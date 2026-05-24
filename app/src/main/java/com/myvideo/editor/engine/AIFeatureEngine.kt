package com.myvideo.editor.engine

import android.content.Context
import android.graphics.Bitmap
import com.myvideo.editor.startup.DeviceTierDetector
import com.myvideo.editor.startup.ModelConfig

/**
 * NexClip AI功能统一管理引擎
 * 根据设备档位自动选择对应精度模型
 */
class AIFeatureEngine(private val context: Context) {

    private val modelDownloader = ModelDownloader(context)
    private var esrganManager: RealESRGANManager? = null
    private var rifeManager: RIFEManager? = null

    data class BeautyParams(
        val smoothSkin: Float = 0f,
        val whiten: Float = 0f,
        val slimFace: Float = 0f,
        val enlargeEyes: Float = 0f,
        val thinNose: Float = 0f,
        val thinBody: Float = 0f
    )

    /**
     * 初始化AI引擎
     */
    fun init(): Boolean {
        return try {
            val tier = DeviceTierDetector.detect(context).tier
            val models = ModelConfig.getModelSet(tier)
            // 模型按需加载，不全部加载到内存
            true
        } catch (e: Exception) { false }
    }

    /**
     * 获取当前设备的模型信息
     */
    fun getDeviceInfo(): String = modelDownloader.getDeviceInfoSummary()

    /**
     * 获取当前设备模型总下载大小
     */
    fun getTotalModelSizeMB(): Int = ModelConfig.getTotalDownloadSizeMB(context)

    /**
     * 获取当前设备可用的最大分辨率
     */
    fun getMaxResolution(): String = ModelConfig.getMaxResolution(context)

    /**
     * 获取当前设备最大帧率
     */
    fun getMaxFps(): Int = ModelConfig.getMaxFps(context)

    /**
     * AI美颜处理
     */
    fun applyBeauty(bitmap: Bitmap, params: BeautyParams): Bitmap? {
        return try {
            val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            if (params.smoothSkin > 0) applySmoothSkin(result, params.smoothSkin)
            if (params.whiten > 0) applyWhiten(result, params.whiten)
            result
        } catch (e: Exception) { null }
    }

    private fun applySmoothSkin(bitmap: Bitmap, strength: Float) {
        val factor = strength / 100f
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16 and 0xFF)
            val g = (p shr 8 and 0xFF)
            val b = (p and 0xFF)
            val avg = (r + g + b) / 3
            val nr = (r + (avg - r) * factor * 0.3f).toInt().coerceIn(0, 255)
            val ng = (g + (avg - g) * factor * 0.3f).toInt().coerceIn(0, 255)
            val nb = (b + (avg - b) * factor * 0.3f).toInt().coerceIn(0, 255)
            pixels[i] = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyWhiten(bitmap: Bitmap, strength: Float) {
        val factor = 1f + strength / 200f
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
            val g = ((p shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
            val b = ((p and 0xFF) * factor).toInt().coerceIn(0, 255)
            pixels[i] = (p and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * AI超分辨率
     */
    fun upscale(bitmap: Bitmap, scale: Int = 4): Bitmap? {
        if (esrganManager == null) {
            esrganManager = RealESRGANManager(context)
            esrganManager?.init()
        }
        return esrganManager?.upscale4x(bitmap)
    }

    /**
     * AI插帧
     */
    fun interpolateFrames(frame1: Bitmap, frame2: Bitmap, t: Float = 0.5f): Bitmap? {
        if (rifeManager == null) {
            rifeManager = RIFEManager(context)
            rifeManager?.init()
        }
        return rifeManager?.interpolate(frame1, frame2, t)
    }

    /**
     * AI慢动作
     */
    fun slowMotion(frames: List<Bitmap>, speed: Float): List<Bitmap> {
        if (rifeManager == null) {
            rifeManager = RIFEManager(context)
            rifeManager?.init()
        }
        return when {
            speed <= 0.25f -> rifeManager?.slowMotion4x(frames) ?: frames
            speed <= 0.5f -> rifeManager?.interpolate2x(frames) ?: frames
            else -> frames
        }
    }

    fun release() {
        esrganManager?.release()
        rifeManager?.release()
    }
}
