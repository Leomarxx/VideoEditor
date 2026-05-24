package com.myvideo.editor.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * NexClip 视频引擎单元测试
 */
class VideoEngineTest {

    // ===== ExportConfig 测试 =====

    @Test
    fun `ExportConfig default values`() {
        val config = VideoEngine.ExportConfig(outputPath = "/tmp/test.mp4")
        assertEquals(1920, config.width)
        assertEquals(1080, config.height)
        assertEquals(8_000_000, config.bitrate)
        assertEquals(30, config.fps)
    }

    @Test
    fun `ExportConfig custom values`() {
        val config = VideoEngine.ExportConfig(
            outputPath = "/tmp/test.mp4",
            width = 3840, height = 2160,
            bitrate = 20_000_000, fps = 60
        )
        assertEquals(3840, config.width)
        assertEquals(2160, config.height)
        assertEquals(60, config.fps)
    }

    // ===== VideoFilterEngine 测试 =====

    @Test
    fun `filter presets return valid params`() {
        val engine = VideoFilterEngine(null)
        val presets = engine.getPresetNames()
        assertTrue("应有预设", presets.isNotEmpty())
        assertTrue("应包含自然", presets.contains("自然"))
        assertTrue("应包含黑白", presets.contains("黑白"))
    }

    @Test
    fun `getColorMatrix returns non-null`() {
        val engine = VideoFilterEngine(null)
        val matrix = engine.getColorMatrix(VideoFilterEngine.FilterParams())
        assertNotNull("矩阵不应为空", matrix)
    }

    @Test
    fun `getPreset returns correct params`() {
        val engine = VideoFilterEngine(null)
        val bw = engine.getPreset("黑白")
        assertEquals("黑白应饱和度-100", -100f, bw.saturation, 0.1f)
    }

    // ===== AIFeatureEngine 测试 =====

    @Test
    fun `beauty params default values`() {
        val params = AIFeatureEngine.BeautyParams()
        assertEquals(0f, params.smoothSkin)
        assertEquals(0f, params.whiten)
        assertEquals(0f, params.slimFace)
        assertEquals(0f, params.enlargeEyes)
    }
}
