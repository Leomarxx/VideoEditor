package com.myvideo.editor.engine

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * NexClip Real-ESRGAN 超分辨率管理器
 * AI 4倍画质增强
 */
class RealESRGANManager(private val context: Context) {

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private val modelDownloader = ModelDownloader(context)

    fun init(): Boolean {
        return try {
            val modelPath = modelDownloader.getModelPath("realesrgan-x4plus.bin") ?: return false
            env = OrtEnvironment.getEnvironment()
            session = env!!.createSession(modelPath)
            true
        } catch (e: Exception) { false }
    }

    /**
     * 4倍超分辨率
     */
    fun upscale4x(bitmap: Bitmap): Bitmap? {
        if (session == null) return null
        return try {
            val input = preprocess(bitmap)
            val tensor = OnnxTensor.createTensor(env, input)
            val results = session!!.run(mapOf("input" to tensor))
            val output = results[0].value as Array<Array<Array<FloatArray>>>
            postprocess(output, bitmap.width * 4, bitmap.height * 4)
        } catch (e: Exception) { null }
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val w = bitmap.width
        val h = bitmap.height
        val input = Array(1) { Array(3) { Array(h) { FloatArray(w) } } }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = bitmap.getPixel(x, y)
                input[0][0][y][x] = (pixel shr 16 and 0xFF) / 255f
                input[0][1][y][x] = (pixel shr 8 and 0xFF) / 255f
                input[0][2][y][x] = (pixel and 0xFF) / 255f
            }
        }
        return input
    }

    private fun postprocess(output: Array<Array<Array<FloatArray>>>, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = (output[0][0][y][x] * 255).toInt().coerceIn(0, 255)
                val g = (output[0][1][y][x] * 255).toInt().coerceIn(0, 255)
                val b = (output[0][2][y][x] * 255).toInt().coerceIn(0, 255)
                bitmap.setPixel(x, y, android.graphics.Color.rgb(r, g, b))
            }
        }
        return bitmap
    }

    fun release() {
        session?.close()
        session = null
    }
}
