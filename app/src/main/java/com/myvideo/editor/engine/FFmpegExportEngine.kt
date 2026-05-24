package com.myvideo.editor.engine

import android.content.Context
import java.io.File

class FFmpegExportEngine(private val context: Context, private val engine: FFmpegRenderEngine) {

    data class ExportProfile(
        val name: String,
        val width: Int,
        val height: Int,
        val fps: Int,
        val videoBitrate: String,
        val audioBitrate: String,
        val codec: String,
        val preset: String,
        val crf: Int
    )

    companion object {
        val PROFILES = mapOf(
            "4K" to ExportProfile("4K", 3840, 2160, 60, "50M", "256k", "libx264", "slow", 18),
            "2K" to ExportProfile("2K", 2560, 1440, 60, "30M", "192k", "libx264", "medium", 20),
            "1080p" to ExportProfile("1080p", 1920, 1080, 30, "16M", "128k", "libx264", "fast", 23),
            "720p" to ExportProfile("720p", 1280, 720, 30, "8M", "128k", "libx264", "fast", 25),
            "480p" to ExportProfile("480p", 854, 480, 24, "4M", "96k", "libx264", "fast", 28)
        )
    }

    fun exportWithProfile(
        input: String, output: String, profileName: String,
        filters: String = "", cb: FFmpegRenderEngine.RenderCallback
    ) {
        val p = PROFILES[profileName] ?: PROFILES["1080p"]!!
        val vf = buildString {
            if (filters.isNotEmpty()) append("$filters,")
            append("scale=${p.width}:${p.height}:force_original_aspect_ratio=decrease,")
            append("pad=${p.width}:${p.height}:(ow-iw)/2:(oh-ih)/2,")
            append("fps=${p.fps}")
        }
        val cmd = "-i $input -vf $vf -c:v ${p.codec} -preset ${p.preset} -crf ${p.crf} -b:v ${p.videoBitrate} -c:a aac -b:a ${p.audioBitrate} -movflags +faststart -y $output"
        engine.run(cmd, cb)
    }

    fun exportGif(input: String, output: String, startMs: Long, endMs: Long,
                  width: Int, fps: Int, cb: FFmpegRenderEngine.RenderCallback) {
        val palette = File(context.cacheDir, "palette.png").absolutePath
        val start = startMs / 1000f
        val dur = (endMs - startMs) / 1000f
        engine.run(
            "-ss $start -t $dur -i $input -vf \"fps=$fps,scale=$width:-1:flags=lanczos,palettegen\" -y $palette",
            object : FFmpegRenderEngine.RenderCallback {
                override fun onProgress(p: Float) {}
                override fun onComplete(o: String) {
                    engine.run("-ss $start -t $dur -i $input -i $palette -filter_complex \"fps=$fps,scale=$width:-1:flags=lanczos[x];[x][1:v]paletteuse\" -y $output", cb)
                }
                override fun onError(e: String) { cb.onError(e) }
                override fun onLog(m: String) {}
            }
        )
    }

    fun exportAudio(input: String, output: String, format: String, bitrate: String,
                    cb: FFmpegRenderEngine.RenderCallback) {
        val codec = when (format) {
            "mp3" -> "-c:a libmp3lame"
            "aac" -> "-c:a aac"
            "wav" -> "-c:a pcm_s16le"
            "flac" -> "-c:a flac"
            else -> "-c:a aac"
        }
        engine.run("-i $input -vn $codec -b:a $bitrate -y $output", cb)
    }

    fun getEstimatedSize(width: Int, height: Int, fps: Int, bitrate: String, durationSec: Float): Long {
        val br = bitrate.replace("M", "000000").replace("k", "000").toLongOrNull() ?: 8000000
        return ((br / 8) * durationSec).toLong()
    }
}
