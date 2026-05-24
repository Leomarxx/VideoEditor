package com.myvideo.editor.startup

import com.myvideo.editor.startup.DeviceTierDetector.Tier
import android.content.Context

object ModelConfig {

    data class ModelVariant(
        val name: String,
        val fileName: String,
        val url: String,
        val sizeMB: Int,
        val isQuantized: Boolean,
        val description: String
    )

    data class ModelSet(
        val tier: Tier,
        val maxResolution: String,
        val maxFps: Int,
        val maxBitrate: String,
        val models: Map<String, ModelVariant>
    )

    // T3 入门机：INT8量化模型，体积小，画质相同，速度慢
    private val T3 = ModelSet(
        tier = Tier.T3,
        maxResolution = "4K",
        maxFps = 60,
        maxBitrate = "32M",
        models = mapOf(
            "sam" to ModelVariant("SAM 2.1 INT8", "sam_vit_h_int8.onnx",
                "https://example.com/sam_vit_h_int8.onnx", 650, true,
                "INT8量化版，画质不变，速度慢15%"),
            "esrgan" to ModelVariant("Real-ESRGAN x4 INT8", "realesrgan-x4plus-int8.bin",
                "https://example.com/realesrgan-x4plus-int8.bin", 17, true,
                "INT8量化版，4倍超分，速度慢20%"),
            "rife" to ModelVariant("RIFE v4.25 INT8", "rife-v4.25-int8.ncnn.bin",
                "https://example.com/rife-v4.25-int8.ncnn.bin", 2, true,
                "INT8量化版，4K插帧，速度慢15%"),
            "whisper" to ModelVariant("Whisper Tiny INT8", "whisper-tiny-int8.onnx",
                "https://example.com/whisper-tiny-int8.onnx", 20, true,
                "INT8量化版，语音识别"),
            "rnnoise" to ModelVariant("RNNoise", "rnnoise_model.rnnn",
                "https://example.com/rnnoise_model.rnnn", 1, false,
                "AI降噪，无量化版")
        )
    )

    // T2 中端机：FP16半精度模型，画质相同，速度快
    private val T2 = ModelSet(
        tier = Tier.T2,
        maxResolution = "4K",
        maxFps = 60,
        maxBitrate = "32M",
        models = mapOf(
            "sam" to ModelVariant("SAM 2.1 FP16", "sam_vit_h_fp16.onnx",
                "https://example.com/sam_vit_h_fp16.onnx", 1300, false,
                "FP16半精度，画质不变，速度适中"),
            "esrgan" to ModelVariant("Real-ESRGAN x4 FP16", "realesrgan-x4plus-fp16.bin",
                "https://example.com/realesrgan-x4plus-fp16.bin", 34, false,
                "FP16半精度，4倍超分"),
            "rife" to ModelVariant("RIFE v4.25 FP16", "rife-v4.25-fp16.ncnn.bin",
                "https://example.com/rife-v4.25-fp16.ncnn.bin", 4, false,
                "FP16半精度，4K插帧"),
            "whisper" to ModelVariant("Whisper Small FP16", "whisper-small-fp16.onnx",
                "https://example.com/whisper-small-fp16.onnx", 230, false,
                "FP16半精度，语音识别"),
            "rnnoise" to ModelVariant("RNNoise", "rnnoise_model.rnnn",
                "https://example.com/rnnoise_model.rnnn", 1, false,
                "AI降噪，无量化版")
        )
    )

    // T1 旗舰机：FP32全精度模型，最大画质，最快速度
    private val T1 = ModelSet(
        tier = Tier.T1,
        maxResolution = "4K",
        maxFps = 60,
        maxBitrate = "32M",
        models = mapOf(
            "sam" to ModelVariant("SAM 2.1 FP32", "sam_vit_h.onnx",
                "https://example.com/sam_vit_h.onnx", 2600, false,
                "FP32全精度，最高画质，最快速度"),
            "esrgan" to ModelVariant("Real-ESRGAN x4 FP32", "realesrgan-x4plus.bin",
                "https://example.com/realesrgan-x4plus.bin", 67, false,
                "FP32全精度，4倍超分"),
            "rife" to ModelVariant("RIFE v4.25 FP32", "rife-v4.25.ncnn.bin",
                "https://example.com/rife-v4.25.ncnn.bin", 7, false,
                "FP32全精度，4K插帧"),
            "whisper" to ModelVariant("Whisper Medium FP32", "whisper-medium.onnx",
                "https://example.com/whisper-medium.onnx", 1500, false,
                "FP32全精度，语音识别"),
            "rnnoise" to ModelVariant("RNNoise", "rnnoise_model.rnnn",
                "https://example.com/rnnoise_model.rnnn", 1, false,
                "AI降噪，无量化版")
        )
    )

    fun getModelSet(tier: Tier): ModelSet = when (tier) {
        Tier.T3 -> T3
        Tier.T2 -> T2
        Tier.T1 -> T1
    }

    fun getModel(context: Context, modelName: String): ModelVariant {
        val tier = DeviceTierDetector.detect(context).tier
        return getModelSet(tier).models[modelName]!!
    }

    fun getTotalDownloadSizeMB(context: Context): Int {
        val tier = DeviceTierDetector.detect(context).tier
        return getModelSet(tier).models.values.sumOf { it.sizeMB }
    }

    fun getMaxResolution(context: Context): String = getModelSet(DeviceTierDetector.detect(context).tier).maxResolution
    fun getMaxFps(context: Context): Int = getModelSet(DeviceTierDetector.detect(context).tier).maxFps
    fun getMaxBitrate(context: Context): String = getModelSet(DeviceTierDetector.detect(context).tier).maxBitrate
}
