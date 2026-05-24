package com.myvideo.editor.engine

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class FFmpegRenderEngine(private val context: Context) {

    interface RenderCallback {
        fun onProgress(percent: Float)
        fun onComplete(outputPath: String)
        fun onError(error: String)
        fun onLog(message: String)
    }

    fun run(command: String, callback: RenderCallback) {
        Thread {
            try {
                callback.onLog("ffmpeg $command")
                FFmpegKit.executeAsync(command) { session ->
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        callback.onProgress(100f)
                        val out = Regex("-y\\s+(.+)").find(command)?.groupValues?.get(1) ?: ""
                        callback.onComplete(out)
                    } else {
                        callback.onError("FFmpeg错误: ${session.failStackTrace}")
                    }
                } { stats ->
                    callback.onProgress((stats.time.toFloat() / 1000).coerceIn(0f, 100f))
                }
            } catch (e: Exception) {
                callback.onError("执行失败: ${e.message}")
            }
        }.start()
    }

    fun export(input: String, output: String, cb: RenderCallback) {
        run("-i $input -c:v libx264 -preset fast -crf 23 -c:a aac -b:a 128k -y $output", cb)
    }

    fun trim(input: String, output: String, startMs: Long, endMs: Long, cb: RenderCallback) {
        run("-i $input -ss ${startMs/1000f} -t ${(endMs-startMs)/1000f} -c:v libx264 -preset fast -c:a aac -y $output", cb)
    }

    fun concat(inputs: List<String>, output: String, cb: RenderCallback) {
        val listFile = File(context.cacheDir, "concat.txt")
        listFile.writeText(inputs.joinToString("\n") { "file '$it'" })
        run("-f concat -safe 0 -i ${listFile.absolutePath} -c copy -y $output", cb)
    }

    fun getMediaInfo(path: String): String? {
        return try { FFprobeKit.getMediaInformation(path).mediaInformation?.format }
        catch (e: Exception) { null }
    }

    fun release() { FFmpegKitConfig.clearSessions() }
}
