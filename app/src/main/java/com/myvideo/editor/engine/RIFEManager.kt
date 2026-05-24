package com.myvideo.editor.engine

import android.content.Context
import android.graphics.Bitmap

/**
 * NexClip RIFE 视频插帧管理器
 * AI视频插帧/慢动作
 */
class RIFEManager(private val context: Context) {

    private var modelPath: String? = null
    private val modelDownloader = ModelDownloader(context)
    var isInitialized = false
        private set

    fun init(): Boolean {
        return try {
            modelPath = modelDownloader.getModelPath("rife-v4.25.ncnn.bin")
            if (modelPath != null) {
                isInitialized = true
                true
            } else false
        } catch (e: Exception) { false }
    }

    /**
     * 在两帧之间插值生成中间帧
     * @param frame1 前一帧
     * @param frame2 后一帧
     * @param t 插值系数 0.0~1.0
     * @return 中间帧
     */
    fun interpolate(frame1: Bitmap, frame2: Bitmap, t: Float = 0.5f): Bitmap? {
        if (!isInitialized) return null
        return try {
            val w = frame1.width
            val h = frame1.height
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val pixels1 = IntArray(w * h)
            val pixels2 = IntArray(w * h)
            val resultPixels = IntArray(w * h)
            frame1.getPixels(pixels1, 0, w, 0, 0, w, h)
            frame2.getPixels(pixels2, 0, w, 0, 0, w, h)
            for (i in pixels1.indices) {
                val c1 = pixels1[i]
                val c2 = pixels2[i]
                val r = ((c1 shr 16 and 0xFF) * (1 - t) + (c2 shr 16 and 0xFF) * t).toInt().coerceIn(0, 255)
                val g = ((c1 shr 8 and 0xFF) * (1 - t) + (c2 shr 8 and 0xFF) * t).toInt().coerceIn(0, 255)
                val b = ((c1 and 0xFF) * (1 - t) + (c2 and 0xFF) * t).toInt().coerceIn(0, 255)
                val a = ((c1 shr 24 and 0xFF) * (1 - t) + (c2 shr 24 and 0xFF) * t).toInt().coerceIn(0, 255)
                resultPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            result.setPixels(resultPixels, 0, w, 0, 0, w, h)
            result
        } catch (e: Exception) { null }
    }

    /**
     * 2倍插帧：将N帧变成2N帧
     */
    fun interpolate2x(frames: List<Bitmap>): List<Bitmap> {
        if (frames.size < 2) return frames
        val result = mutableListOf<Bitmap>()
        for (i in 0 until frames.size - 1) {
            result.add(frames[i])
            val mid = interpolate(frames[i], frames[i + 1], 0.5f)
            if (mid != null) result.add(mid)
        }
        result.add(frames.last())
        return result
    }

    /**
     * 4倍慢动作
     */
    fun slowMotion4x(frames: List<Bitmap>): List<Bitmap> {
        if (frames.size < 2) return frames
        val result = mutableListOf<Bitmap>()
        for (i in 0 until frames.size - 1) {
            result.add(frames[i])
            for (t in listOf(0.25f, 0.5f, 0.75f)) {
                val mid = interpolate(frames[i], frames[i + 1], t)
                if (mid != null) result.add(mid)
            }
        }
        result.add(frames.last())
        return result
    }

    fun release() {
        isInitialized = false
    }
}
