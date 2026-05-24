package com.myvideo.editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.nio.ByteBuffer

/**
 * NexClip 滤镜引擎
 * 亮度/对比度/饱和度/色温/锐化
 */
class VideoFilterEngine(private val context: Context) {

    data class FilterParams(
        val brightness: Float = 0f,    // -100~100
        val contrast: Float = 0f,      // -100~100
        val saturation: Float = 0f,    // -100~100
        val temperature: Float = 0f,   // -100~100
        val sharpen: Float = 0f,       // 0~100
        val vignette: Float = 0f,      // 0~100
        val hue: Float = 0f            // -180~180
    )

    /**
     * 应用滤镜到Bitmap（CPU处理，用于预览）
     */
    fun applyFilter(source: Bitmap, params: FilterParams): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 亮度+对比度+饱和度
        val brightness = params.brightness / 100f * 255f
        val contrastScale = 1f + params.contrast / 100f
        val saturation = 1f + params.saturation / 100f

        val cm = ColorMatrix(floatArrayOf(
            contrastScale * saturation, 0f, 0f, 0f, brightness,
            0f, contrastScale * saturation, 0f, 0f, brightness,
            0f, 0f, contrastScale * saturation, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        // 色温
        if (params.temperature != 0f) {
            val temp = params.temperature / 100f
            val tempMatrix = ColorMatrix(floatArrayOf(
                1f + temp * 0.3f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - temp * 0.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(tempMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return result
    }

    /**
     * 获取ColorMatrix（Compose UI用）
     */
    fun getColorMatrix(params: FilterParams): ColorMatrix {
        val brightness = params.brightness / 100f
        val contrast = 1f + params.contrast / 100f
        val sat = 1f + params.saturation / 100f
        val b = brightness * 255f

        return ColorMatrix(floatArrayOf(
            contrast * sat, 0f, 0f, 0f, b,
            0f, contrast * sat, 0f, 0f, b,
            0f, 0f, contrast * sat, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    /**
     * 预设滤镜
     */
    fun getPreset(name: String): FilterParams {
        return when (name) {
            "自然" -> FilterParams(brightness = 5f, contrast = 10f, saturation = 15f)
            "胶片" -> FilterParams(brightness = -5f, contrast = 20f, saturation = -20f, temperature = 15f)
            "冷色" -> FilterParams(temperature = -30f, saturation = -10f)
            "暖色" -> FilterParams(temperature = 30f, saturation = 10f)
            "黑白" -> FilterParams(saturation = -100f, contrast = 15f)
            "高对比" -> FilterParams(contrast = 40f, brightness = -10f)
            "鲜艳" -> FilterParams(saturation = 50f, contrast = 15f)
            "复古" -> FilterParams(brightness = -10f, contrast = -10f, saturation = -30f, temperature = 20f)
            else -> FilterParams()
        }
    }

    fun getPresetNames(): List<String> {
        return listOf("自然", "胶片", "冷色", "暖色", "黑白", "高对比", "鲜艳", "复古")
    }
}
