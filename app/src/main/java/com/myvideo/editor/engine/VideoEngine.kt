package com.myvideo.editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.net.Uri
import android.view.Surface
import java.io.File

/**
 * NexClip 视频引擎：导入/解码/导出
 */
class VideoEngine(private val context: Context) {

    data class VideoInfo(
        val uri: Uri, val width: Int, val height: Int,
        val durationMs: Long, val bitrate: Int, val fps: Int,
        val mimeType: String, val rotation: Int, val hasAudio: Boolean
    )

    data class ExportConfig(
        val outputPath: String, val width: Int = 1920, val height: Int = 1080,
        val bitrate: Int = 8_000_000, val fps: Int = 30,
        val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        val audioBitrate: Int = 128_000
    )

    interface ProgressCallback {
        fun onProgress(percent: Float)
        fun onComplete(outputPath: String)
        fun onError(error: String)
    }

    // ===== 视频导入 =====

    fun getVideoInfo(uri: Uri): VideoInfo? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var width = 0; var height = 0; var durationMs = 0L
            var bitrate = 0; var fps = 30; var mimeType = ""
            var rotation = 0; var hasAudio = false

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000
                    bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE))
                        format.getInteger(MediaFormat.KEY_BIT_RATE) else 8_000_000
                    fps = if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
                        format.getInteger(MediaFormat.KEY_FRAME_RATE) else 30
                    mimeType = mime
                    rotation = if (format.containsKey(MediaFormat.KEY_ROTATION))
                        format.getInteger(MediaFormat.KEY_ROTATION) else 0
                } else if (mime.startsWith("audio/")) {
                    hasAudio = true
                }
            }
            if (mimeType.isEmpty()) return null
            return VideoInfo(uri, width, height, durationMs, bitrate, fps, mimeType, rotation, hasAudio)
        } catch (e: Exception) { return null }
        finally { extractor.release() }
    }

    fun getDurationMs(uri: Uri): Long = getVideoInfo(uri)?.durationMs ?: 0L

    private fun findVideoTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) return i
        }
        return null
    }

    // ===== 视频导出 =====

    fun exportVideo(inputUri: Uri, config: ExportConfig, callback: ProgressCallback) {
        Thread {
            val extractor = MediaExtractor()
            var videoEncoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            try {
                extractor.setDataSource(context, inputUri, null)
                val videoTrack = findVideoTrack(extractor)
                if (videoTrack == null) { callback.onError("无视频轨道"); return@Thread }

                extractor.selectTrack(videoTrack)
                val inputFormat = extractor.getTrackFormat(videoTrack)
                val inputDuration = inputFormat.getLong(MediaFormat.KEY_DURATION)

                File(config.outputPath).parentFile?.mkdirs()

                // 视频编码器
                val outputFormat = MediaFormat.createVideoFormat(
                    config.mimeType, config.width, config.height
                ).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                }
                videoEncoder = MediaCodec.createEncoderByType(config.mimeType)
                videoEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                videoEncoder.start()

                muxer = MediaMuxer(config.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var muxerVideoTrack = -1
                var muxerStarted = false

                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                val timeoutUs = 10_000L

                while (true) {
                    // 输入
                    if (!inputDone) {
                        val idx = videoEncoder.dequeueInputBuffer(timeoutUs)
                        if (idx >= 0) {
                            val buf = videoEncoder.getInputBuffer(idx) ?: continue
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
                                videoEncoder.queueInputBuffer(idx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                videoEncoder.queueInputBuffer(idx, 0, size,
                                    extractor.sampleTime, 0)
                                val progress = (extractor.sampleTime.toFloat() / inputDuration * 100)
                                    .coerceIn(0f, 100f)
                                callback.onProgress(progress)
                                extractor.advance()
                            }
                        }
                    }

                    // 输出
                    val outIdx = videoEncoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        muxerVideoTrack = muxer.addTrack(videoEncoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIdx >= 0) {
                        val outBuf = videoEncoder.getOutputBuffer(outIdx) ?: continue
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            if (muxerStarted) muxer.writeSampleData(muxerVideoTrack, outBuf, bufferInfo)
                        }
                        videoEncoder.releaseOutputBuffer(outIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }

                callback.onProgress(100f)
                callback.onComplete(config.outputPath)
            } catch (e: Exception) {
                callback.onError("导出失败: ${e.message}")
            } finally {
                try { videoEncoder?.stop(); videoEncoder?.release() } catch (e: Exception) { }
                try { muxer?.stop(); muxer?.release() } catch (e: Exception) { }
                extractor.release()
            }
        }.start()
    }

    // ===== 缩略图提取 =====

    fun extractThumbnails(uri: Uri, count: Int): List<Bitmap?> {
        val info = getVideoInfo(uri) ?: return emptyList()
        val interval = info.durationMs / count
        return (0 until count).map { i ->
            extractFrame(uri, i * interval)
        }
    }

    fun extractFrame(uri: Uri, timeMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(timeMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) { null }
        finally { retriever.release() }
    }
}
