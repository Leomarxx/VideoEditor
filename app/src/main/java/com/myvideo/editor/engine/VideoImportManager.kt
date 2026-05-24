package com.myvideo.editor.engine

import android.content.Context
import android.net.Uri
import com.myvideo.editor.ui.editor.ClipData
import com.myvideo.editor.ui.editor.ClipType
import com.myvideo.editor.ui.editor.EditorViewModel

class VideoImportManager(private val context: Context) {

    private val engine = VideoEngine(context)
    val importedVideos = mutableListOf<VideoEngine.VideoInfo>()

    fun importToEditor(uri: Uri, vm: EditorViewModel): Boolean {
        val info = engine.getVideoInfo(uri) ?: return false
        importedVideos.add(info)

        val durationSec = info.durationMs / 1000f
        val clipWidth = durationSec * vm.pixelsPerSecond

        val lastRight = vm.clips.maxOfOrNull { it.leftPx + it.widthPx } ?: vm.rulerStartPx
        val gap = 10f

        val clipId = "clip_${System.currentTimeMillis()}"
        val clip = ClipData(
            id = clipId,
            name = uri.lastPathSegment ?: "video.mp4",
            leftPx = lastRight + gap,
            widthPx = clipWidth,
            trackIndex = vm.selectedTrackIndex,
            type = ClipType.Video
        )

        vm.clips.add(clip)
        // 保存URI供导出使用
        vm.videoUris[clipId] = uri.toString()
        return true
    }

    fun getEngine() = engine
    fun getThumbnails(uri: Uri, count: Int = 10) = engine.extractThumbnails(uri, count)
    fun getInfo(uri: Uri) = engine.getVideoInfo(uri)
}
