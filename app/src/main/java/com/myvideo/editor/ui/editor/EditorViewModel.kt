package com.myvideo.editor.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

// ===== 数据模型 =====
data class ClipData(
    val id: String,
    val name: String,
    val leftPx: Float,
    val widthPx: Float,
    val trackIndex: Int,
    val type: ClipType = ClipType.Video,
    val colorStart: Color = Color(0x264A90D9),
    val colorEnd: Color = Color(0x404A90D9),
    val borderColor: Color = Color(0x334A90D9),
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val keyframes: MutableList<Float> = mutableListOf(),
    val speed: Float = 1f,
    val isReversed: Boolean = false
)

enum class ClipType { Video, Audio, Subtitle, Group, Adjustment }

data class TrackData(
    val index: Int,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

data class MaskPoint(
    val x: Float, val y: Float,
    val cx1: Float, val cy1: Float,
    val cx2: Float, val cy2: Float
)

// ===== 状态管理 =====
class EditorViewModel {
    // 预览区
    var canvasRatio by mutableStateOf("16:9")
    var customWidth by mutableStateOf("1920")
    var customHeight by mutableStateOf("1080")
    var previewHeightPx by mutableStateOf(180f)

    // 播放
    var isPlaying by mutableStateOf(false)
    var playheadPosition by mutableStateOf(135f)
    var currentTime by mutableStateOf("00:01.10")
    var totalDuration by mutableStateOf("00:01.70")
    var pixelsPerSecond by mutableStateOf(80f)
    val rulerStartPx = 30f

    // 轨道
    val tracks = mutableStateListOf(
        TrackData(0, "视频 1")
    )
    var selectedTrackIndex by mutableStateOf(0)

    // 片段
    val clips = mutableStateListOf(
        ClipData("v1a", "Scene_01.mp4", 30f, 140f, 0, ClipType.Video, keyframes = mutableListOf(45f, 85f, 125f)),
        ClipData("v1b", "Scene_02.mp4", 200f, 100f, 0, ClipType.Video)
    )
    var selectedClipId by mutableStateOf<String?>(null)

    // 视频URI存储（clipId -> uri）
    val videoUris = mutableMapOf<String, String>()

    // 导出状态
    var exportProgress by mutableStateOf(0f)
    var isExporting by mutableStateOf(false)
    var exportDone by mutableStateOf(false)
    var exportError by mutableStateOf<String?>(null)

    // 播放器（ExoPlayer）
    var playerReady by mutableStateOf(false)
    var playerDurationMs by mutableStateOf(0L)
    var playerCurrentMs by mutableStateOf(0L)
    var playerIsPlaying by mutableStateOf(false)

    // 钢笔蒙版
    var penMode by mutableStateOf(false)
    val maskPoints = mutableStateListOf<MaskPoint>()
    var maskClosed by mutableStateOf(false)

    // 弹出面板
    var activePanel by mutableStateOf<String?>(null)

    // FAB菜单
    var showFxPopup by mutableStateOf(false)

    // 右键菜单
    var showContextMenu by mutableStateOf(false)
    var contextMenuPosition by mutableStateOf(Offset.Zero)
    var contextMenuTargetId by mutableStateOf<String?>(null)

    // Toast
    var toastMessage by mutableStateOf("")
    var showToast by mutableStateOf(false)

    // 轨道面板折叠
    var trackPanelCollapsed by mutableStateOf(false)

    // 免费计数
    var freeUsed by mutableStateOf(3)
    val freeMax = 5

    // 撤销栈
    val undoStack = mutableListOf<String>()
    val redoStack = mutableListOf<String>()

    // 时间轴播放头高度
    val timelinePlayheadHeight: Int get() = tracks.size * 48

    // ===== 辅助函数 =====
    fun formatTime(seconds: Float): String {
        val mins = (seconds / 60).toInt()
        val secs = seconds % 60
        return "%02d:%05.2f".format(mins, secs)
    }

    fun selectedClip(): ClipData? = clips.find { it.id == selectedClipId }

    fun addTrack() {
        val idx = tracks.size
        tracks.add(TrackData(idx, "视频 ${idx + 1}"))
    }

    fun deleteSelectedClip() {
        val id = selectedClipId ?: return
        clips.removeAll { it.id == id }
        selectedClipId = null
    }

    fun splitSelectedClip() {
        val clip = selectedClip() ?: return
        val relPos = playheadPosition - clip.leftPx
        if (relPos <= 0 || relPos >= clip.widthPx) return
        val newId = "${clip.id}_split_${System.currentTimeMillis()}"
        val newClip = clip.copy(
            id = newId, name = clip.name,
            leftPx = playheadPosition,
            widthPx = clip.widthPx - relPos,
            keyframes = mutableListOf()
        )
        val idx = clips.indexOf(clip)
        clips[idx] = clip.copy(widthPx = relPos)
        clips.add(idx + 1, newClip)
    }

    fun addKeyframe() {
        val clip = selectedClip() ?: return
        val relPos = playheadPosition - clip.leftPx
        if (relPos < 0 || relPos > clip.widthPx) return
        clip.keyframes.add(relPos)
        clip.keyframes.sort()
    }

    fun incrementFree() {
        freeUsed = (freeUsed + 1).coerceAtMost(freeMax)
    }

    fun showToast(msg: String) {
        toastMessage = msg; showToast = true
    }

    fun movePlayhead(x: Float) {
        playheadPosition = x.coerceAtLeast(rulerStartPx)
        val seconds = ((playheadPosition - rulerStartPx) / pixelsPerSecond).coerceAtLeast(0f)
        currentTime = formatTime(seconds)
    }

    fun stepForward() { movePlayhead(playheadPosition + pixelsPerSecond) }
    fun stepBackward() { movePlayhead((playheadPosition - pixelsPerSecond).coerceAtLeast(rulerStartPx)) }

    fun getCanvasRatioFloat(): Float {
        return when (canvasRatio) {
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "1:1" -> 1f
            "4:3" -> 4f / 3f
            "21:9" -> 21f / 9f
            "自定义" -> {
                val w = customWidth.toFloatOrNull() ?: 1920f
                val h = customHeight.toFloatOrNull() ?: 1080f
                if (h > 0) w / h else 16f / 9f
            }
            else -> 16f / 9f
        }
    }
}
