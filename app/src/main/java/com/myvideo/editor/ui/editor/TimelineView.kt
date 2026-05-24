package com.myvideo.editor.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object TC {
    val Surf = Color(0xFF282828); val Card = Color(0xFF2C2C2C)
    val Line = Color(0xFF3A3A3A); val Line2 = Color(0xFF444444)
    val T1 = Color(0xFFCCCCCC); val T2 = Color(0xFF999999)
    val T3 = Color(0xFF666666); val T4 = Color(0xFF4A4A4A)
    val Acc = Color(0xFF4A90D9); val AccL = Color(0xFF6AAFE6)
    val AccS = Color(0x1F4A90D9); val Gold = Color(0xFFE8A820)
    val Green = Color(0xFF7EC850); val Red = Color(0xFFE85050)
}

@Composable
fun TimelineView(vm: EditorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 刻度尺
        RulerBar(vm)
        // 轨道区
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 轨道头
            TrackPanel(vm)
            // 折叠按钮
            Box(modifier = Modifier.width(14.dp).fillMaxHeight().background(TC.Surf)
                .border(1.dp, TC.Line).clickable { vm.trackPanelCollapsed = !vm.trackPanelCollapsed },
                contentAlignment = Alignment.Center) {
                Text(if (vm.trackPanelCollapsed) "▶" else "◀", fontSize = 8.sp, color = TC.T3)
            }
            // 时间轴内容
            TimelineContent(vm)
        }
    }
}

@Composable
private fun RulerBar(vm: EditorViewModel) {
    val totalWidth = (400 + vm.tracks.size * 48 * 8).dp

    Box(modifier = Modifier.fillMaxWidth().height(26.dp).background(TC.Surf).border(1.dp, TC.Line)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).width(totalWidth)) {
            Canvas(modifier = Modifier.fillMaxHeight().width(totalWidth)) {
                val h = size.height
                // 主刻度线
                val totalSec = ((size.width - vm.rulerStartPx) / vm.pixelsPerSecond).toInt()
                for (s in 0..totalSec) {
                    val x = vm.rulerStartPx + s * vm.pixelsPerSecond
                    drawLine(TC.Line2, Offset(x, h - 8f), Offset(x, h), strokeWidth = 1f)
                    // 次刻度
                    val subX = x + vm.pixelsPerSecond / 2
                    if (subX < size.width) {
                        drawLine(TC.Line2, Offset(subX, h - 4f), Offset(subX, h), strokeWidth = 1f)
                    }
                }
                // 播放头
                drawLine(Color.White, Offset(vm.playheadPosition, 0f), Offset(vm.playheadPosition, h), strokeWidth = 2f)
                // 三角指示器
                val triSize = 5f
                drawPath(
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(vm.playheadPosition, 0f)
                        lineTo(vm.playheadPosition - triSize, triSize * 2)
                        lineTo(vm.playheadPosition + triSize, triSize * 2)
                        close()
                    },
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TrackPanel(vm: EditorViewModel) {
    if (vm.trackPanelCollapsed) return

    Column(modifier = Modifier.width(90.dp).verticalScroll(rememberScrollState())
        .background(TC.Surf).border(1.dp, TC.Line)) {
        vm.tracks.forEachIndexed { idx, track ->
            val isSelected = vm.selectedTrackIndex == idx
            Row(modifier = Modifier.fillMaxWidth().height(48.dp)
                .background(if (isSelected) TC.AccS else Color.Transparent)
                .then(if (isSelected) Modifier.border(2.dp, TC.Acc, RoundedCornerShape(0.dp)) else Modifier)
                .clickable { vm.selectedTrackIndex = idx }
                .padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, fontSize = 7.5.sp, color = TC.T2, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        TrackSwitch("S", track.isSolo, TC.Acc, TC.AccL) {}
                        TrackSwitch("M", track.isMuted, TC.Red, TC.Red) {}
                        TrackSwitch("E", track.isVisible, TC.Acc, TC.AccL) {}
                        TrackSwitch("L", track.isLocked, TC.T4, TC.T2) {}
                    }
                }
            }
        }
        // 添加轨道
        Row(modifier = Modifier.fillMaxWidth().height(32.dp).clickable { vm.addTrack() }
            .border(1.dp, TC.Line), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("+ 添加轨道", fontSize = 8.sp, color = TC.T4, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TrackSwitch(label: String, isOn: Boolean, onColor: Color, activeColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
        .background(if (isOn) TC.AccS else Color.Transparent)
        .then(if (isOn) Modifier.border(1.dp, TC.Acc.copy(alpha = 0.2f), RoundedCornerShape(3.dp)) else Modifier)
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold,
            color = if (isOn) activeColor else TC.T4)
    }
}

@Composable
private fun TimelineContent(vm: EditorViewModel) {
    val totalWidth = (400 + vm.tracks.size * 48 * 8).dp
    val hScroll = rememberScrollState()

    Box(modifier = Modifier.weight(1f).fillMaxHeight()
        .horizontalScroll(hScroll).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.width(totalWidth).height((vm.tracks.size * 48).dp)) {
            // 片段
            vm.tracks.forEachIndexed { trackIdx, _ ->
                val trackClips = vm.clips.filter { it.trackIndex == trackIdx }
                trackClips.forEach { clip ->
                    ClipView(vm = vm, clip = clip, trackTop = trackIdx * 48f)
                }
            }
            // 播放头线
            Box(modifier = Modifier.offset(vm.playheadPosition.dp, 0.dp)
                .width(2.dp).height((vm.tracks.size * 48).dp).background(Color.White))
            // 节拍标记
            val totalWidthPx = 400f + vm.tracks.size * 48 * 8
            val beatPx = (60f / 128f) * vm.pixelsPerSecond
            var bx = vm.rulerStartPx
            while (bx < totalWidthPx) {
                Box(modifier = Modifier.offset(bx.dp, 0.dp)
                    .width(1.5.dp).height((vm.tracks.size * 48).dp)
                    .background(TC.Gold.copy(alpha = 0.35f)))
                bx += beatPx
            }
        }
    }
}

@Composable
private fun ClipView(vm: EditorViewModel, clip: ClipData, trackTop: Float) {
    val isSelected = vm.selectedClipId == clip.id
    val bgBrush = Brush.horizontalGradient(listOf(clip.colorStart, clip.colorEnd))

    Box(modifier = Modifier.offset(clip.leftPx.dp, (trackTop + 6).dp)
        .width(clip.widthPx.dp).height(36.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(bgBrush)
        .then(if (isSelected) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier.border(1.dp, clip.borderColor, RoundedCornerShape(4.dp)))
        .clickable { vm.selectedClipId = if (isSelected) null else clip.id }
        .pointerInput(clip.id) {
            detectTapGestures(
                onTap = { vm.selectedClipId = if (isSelected) null else clip.id },
                onLongPress = {
                    vm.contextMenuTargetId = clip.id
                    vm.contextMenuPosition = it
                    vm.showContextMenu = true
                }
            )
        }
        .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 片段名称
        Text(clip.name, fontSize = 7.5.sp, color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // 静音遮罩
        if (clip.isMuted) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }
        // 关键帧菱形
        if (isSelected) {
            clip.keyframes.forEach { kfX ->
                Box(modifier = Modifier.offset(kfX.dp, 0.dp).size(6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .align(Alignment.CenterStart))
            }
        }
        // 编组标记
        if (clip.type == ClipType.Group) {
            Text("GRP", fontSize = 5.sp, color = TC.AccL, fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopStart).padding(1.dp))
        }
    }
}

// 创建不同类型片段的辅助函数
fun createVideoClip(name: String, left: Float, width: Float, track: Int): ClipData {
    return ClipData(
        id = "clip_${System.currentTimeMillis()}_${(0..9999).random()}",
        name = name, leftPx = left, widthPx = width, trackIndex = track,
        type = ClipType.Video,
        colorStart = Color(0x264A90D9), colorEnd = Color(0x404A90D9),
        borderColor = Color(0x334A90D9)
    )
}

fun createSubtitleClip(name: String, left: Float, width: Float, track: Int): ClipData {
    return ClipData(
        id = "sub_${System.currentTimeMillis()}_${(0..9999).random()}",
        name = name, leftPx = left, widthPx = width, trackIndex = track,
        type = ClipType.Subtitle,
        colorStart = Color(0x1AE8A820), colorEnd = Color(0x2EE8A820),
        borderColor = Color(0x2EE8A820)
    )
}

fun createAudioClip(name: String, left: Float, width: Float, track: Int): ClipData {
    return ClipData(
        id = "aud_${System.currentTimeMillis()}_${(0..9999).random()}",
        name = name, leftPx = left, widthPx = width, trackIndex = track,
        type = ClipType.Audio,
        colorStart = Color(0x147EC850), colorEnd = Color(0x147EC850),
        borderColor = Color(0x1F7EC850)
    )
}

fun createGroupClip(name: String, left: Float, width: Float, track: Int): ClipData {
    return ClipData(
        id = "grp_${System.currentTimeMillis()}_${(0..9999).random()}",
        name = name, leftPx = left, widthPx = width, trackIndex = track,
        type = ClipType.Group,
        colorStart = Color(0x404A90D9), colorEnd = Color(0x596AAFE6),
        borderColor = Color(0x666AAFE6)
    )
}
