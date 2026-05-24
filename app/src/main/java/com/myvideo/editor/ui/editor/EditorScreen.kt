package com.myvideo.editor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.ui.editor.panels.*
import com.myvideo.editor.engine.VideoPlayerManager
import com.myvideo.editor.engine.rememberVideoPlayer
import android.net.Uri

private object EC {
    val Bg = Color(0xFF1E1E1E); val Surf = Color(0xFF282828)
    val Card = Color(0xFF2C2C2C); val CardH = Color(0xFF323232)
    val Line = Color(0xFF3A3A3A); val Line2 = Color(0xFF444444)
    val T1 = Color(0xFFCCCCCC); val T2 = Color(0xFF999999)
    val T3 = Color(0xFF666666); val T4 = Color(0xFF4A4A4A)
    val Acc = Color(0xFF4A90D9); val AccL = Color(0xFF6AAFE6)
    val AccS = Color(0x1F4A90D9); val Gold = Color(0xFFE8A820)
    val Green = Color(0xFF7EC850); val Red = Color(0xFFE85050)
}

@Composable
fun EditorScreen(vm: EditorViewModel = EditorViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playerManager = rememberVideoPlayer(context, vm)
        if (vm.showToast) { kotlinx.coroutines.delay(2000); vm.showToast = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(EC.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 预览区
            Box(modifier = Modifier.fillMaxWidth().height(vm.previewHeightPx.dp).background(Color.Black)) {
                PreviewCanvas(vm)
            }
            // 拖拽手柄
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(EC.Surf)
                .border(1.dp, EC.Line), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(EC.Line2))
            }
            // 工具栏
            EditorToolbar(vm)
            // 时间轴
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(EC.Card)) {
                TimelineView(vm)
            }
            // 播放控制栏
            PlaybackBar(vm)
        }
        // FAB
        if (vm.selectedClipId != null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp)
                .size(36.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(EC.Acc, EC.AccL)))
                .clickable { vm.showFxPopup = !vm.showFxPopup }, contentAlignment = Alignment.Center) {
                Text("+", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        // Toast
        if (vm.showToast) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                .clip(RoundedCornerShape(10.dp)).background(EC.CardH)
                .border(1.dp, EC.Line2, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(vm.toastMessage, fontSize = 11.sp, color = EC.T1)
            }
        }
        // 右键菜单
        if (vm.showContextMenu) {
            Box(modifier = Modifier.fillMaxSize()
                .clickable { vm.showContextMenu = false }) {
                Column(modifier = Modifier
                    .offset((vm.contextMenuPosition.x / 2).dp, (vm.contextMenuPosition.y / 2).dp)
                    .width(130.dp).clip(RoundedCornerShape(10.dp)).background(EC.CardH)
                    .border(1.dp, EC.Line2, RoundedCornerShape(10.dp)).padding(4.dp)) {
                    CtxItem("复制") { vm.showToast("已复制"); vm.showContextMenu = false }
                    CtxItem("粘贴") { vm.showToast("已粘贴"); vm.showContextMenu = false }
                    CtxItem("添加关键帧") { vm.addKeyframe(); vm.showContextMenu = false }
                    CtxItem("速度") { vm.activePanel = "speed"; vm.showContextMenu = false }
                    CtxItem("删除", true) {
                        vm.deleteSelectedClip()
                        vm.showContextMenu = false
                        vm.showToast("已删除")
                    }
                }
            }
        }
        // 面板
        PanelOverlay(vm)
    }
}

@Composable
private fun PreviewCanvas(vm: EditorViewModel) {
    val ratio = vm.getCanvasRatioFloat()
    val isLandscape = ratio >= 1f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val w = if (isLandscape) 0.95f else 0.5f
        val h = if (!isLandscape) 0.95f else 0.5f
        Box(modifier = Modifier.fillMaxWidth(w).fillMaxHeight(h)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(
                Color(0xFF1A1A20), Color(0xFF0A1520), Color(0xFF101818)))),
            contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.3f).fillMaxHeight(0.3f)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50)))
            Box(modifier = Modifier.fillMaxWidth(0.44f).fillMaxHeight(0.3f)
                .clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)))
            Box(modifier = Modifier.fillMaxWidth(0.3f).fillMaxHeight(0.2f)
                .clip(RoundedCornerShape(50)).background(Color(0x0F6496FF)))
        }
        // 分辨率标签
        val resText = if (vm.canvasRatio == "自定义") "${vm.customWidth}×${vm.customHeight}"
        else when(vm.canvasRatio) {
            "16:9" -> "1920×1080"; "9:16" -> "1080×1920"; "1:1" -> "1080×1080"
            "4:3" -> "1440×1080"; "21:9" -> "2560×1080"; else -> "1920×1080"
        }
        Text(resText, fontSize = 7.sp, color = EC.T3, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.55f))
                .padding(5.dp, 2.dp))
        // 帧率
        Text("30fps", fontSize = 7.sp, color = EC.T3, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.55f))
                .padding(5.dp, 2.dp))
    }
}

@Composable
private fun PlaybackBar(vm: EditorViewModel) {
    Row(modifier = Modifier.fillMaxWidth().height(44.dp).background(EC.Surf)
        .border(1.dp, EC.Line),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Text(vm.currentTime, fontSize = 9.sp, color = EC.T2, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Spacer(modifier = Modifier.width(10.dp))
        PlaybackBtn("⏮") { playerManager?.stepBackward() ?: vm.stepBackward() }
        PlaybackBtn("◀◀") { playerManager?.stepBackward() ?: vm.stepBackward() }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(EC.Acc, EC.AccL)))
            .clickable { if (playerManager != null) {
                            playerManager?.togglePlay()
                            vm.playerIsPlaying = playerManager?.isPlaying ?: false
                        } else { vm.isPlaying = !vm.isPlaying } }, contentAlignment = Alignment.Center) {
            Text(if (vm.isPlaying) "⏸" else "▶", fontSize = 14.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(10.dp))
        PlaybackBtn("▶▶") { playerManager?.stepForward() ?: vm.stepForward() }
        PlaybackBtn("⏭") { playerManager?.stepForward() ?: vm.stepForward() }
        Spacer(modifier = Modifier.width(10.dp))
        Text(vm.totalDuration, fontSize = 9.sp, color = EC.T4, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PlaybackBtn(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 12.sp, color = EC.T3)
    }
}

@Composable
private fun CtxItem(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
        .clickable { onClick() }.padding(8.dp, 8.dp, 12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = if (danger) EC.Red else EC.T2)
    }
}

private fun EditorViewModel.showToast(msg: String) {
    toastMessage = msg; showToast = true
}

@Composable
private fun EditorToolbar(vm: EditorViewModel) {
    Row(modifier = Modifier.fillMaxWidth().height(40.dp).background(EC.Surf)
        .border(1.dp, EC.Line).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ToolbarBtn("撤销") { vm.showToast("已撤销") }
        ToolbarBtn("重做") { vm.showToast("已重做") }
        Spacer(modifier = Modifier.width(1.dp).height(16.dp).background(EC.Line2))
        ToolbarBtn("分割") { vm.splitSelectedClip(); vm.showToast("已分割") }
        ToolbarBtn("删除") { vm.deleteSelectedClip(); vm.showToast("已删除") }
        Spacer(modifier = Modifier.width(1.dp).height(16.dp).background(EC.Line2))
        ToolbarBtn("钢笔", vm.penMode) { vm.penMode = !vm.penMode }
        ToolbarBtn("清除") { vm.maskPoints.clear(); vm.maskClosed = false }
        Spacer(modifier = Modifier.width(1.dp).height(16.dp).background(EC.Line2))
        ToolbarBtn("关键帧") { vm.addKeyframe(); vm.showToast("已添加关键帧") }
        ToolbarBtn("调色") { vm.activePanel = "color" }
        ToolbarBtn("效果") { vm.activePanel = "fx" }
        ToolbarBtn("文字") { vm.activePanel = "text" }
        ToolbarBtn("音频") { vm.activePanel = "audio" }
        Spacer(modifier = Modifier.width(1.dp).height(16.dp).background(EC.Line2))
        ToolbarBtn("混合") { vm.activePanel = "blend" }
        ToolbarBtn("导出") { vm.activePanel = "export" }
        ToolbarBtn("转场") { vm.activePanel = "transitions" }
        Spacer(modifier = Modifier.weight(1f))
        Text("免费 ", fontSize = 9.sp, color = EC.T3, fontWeight = FontWeight.Medium)
        Text("${vm.freeUsed}", fontSize = 9.sp, color = EC.Gold, fontWeight = FontWeight.Bold)
        Text("/${vm.freeMax}", fontSize = 9.sp, color = EC.T3, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToolbarBtn(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp))
        .background(if (active) EC.AccS else Color.Transparent)
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 8.sp, color = if (active) EC.AccL else EC.T2,
            fontWeight = FontWeight.Medium)
    }
}

// ===== 面板系统 =====
@Composable
private fun PanelOverlay(vm: EditorViewModel) {
    if (vm.activePanel == null) return
    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000))
        .clickable { vm.activePanel = null }) {
        Column(modifier = Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(EC.Surf).clickable { }) {
            Row(modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp)
                .border(1.dp, EC.Line),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(when (vm.activePanel) {
                    "color" -> "调色 · AE级"
                    "speed" -> "变速"
                    "text" -> "文字编辑"
                    "fx" -> "效果"
                    "blend" -> "混合模式"
                    "audio" -> "音频"
                    "export" -> "导出"
                    "transitions" -> "转场"
                    "tracking" -> "运动追踪"
                    "stabilizer" -> "视频稳定器"
                    "pip" -> "画中画"
                    "chroma" -> "绿幕抠像"
                    "motionblur" -> "动态模糊"
                    "particles" -> "粒子效果"
                    "lens" -> "镜头效果"
                    "film" -> "胶片颗粒"
                    "template" -> "项目模板"
                    else -> "面板"
                }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EC.T1)
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    .background(EC.Card).clickable { vm.activePanel = null },
                    contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 14.sp, color = EC.T3)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                when (vm.activePanel) {
                    "color" -> ColorGradingPanel(vm = vm) { vm.activePanel = null }
                    "speed" -> SpeedPanel(vm = vm) { vm.activePanel = null }
                    "text" -> TextPanel(vm = vm) { vm.activePanel = null }
                    "fx" -> EffectsPanel(vm = vm) { vm.activePanel = null }
                    "blend" -> BlendPanel(vm = vm) { vm.activePanel = null }
                    "audio" -> AudioPanel(vm = vm) { vm.activePanel = null }
                    "export" -> ExportPanel(vm = vm) { vm.activePanel = null }
                    "transitions" -> TransitionPanel(vm = vm) { vm.activePanel = null }
                    "tracking" -> TrackingPanel(vm = vm) { vm.activePanel = null }
                    "stabilizer" -> StabilizerPanel(vm = vm) { vm.activePanel = null }
                    "pip" -> PiPPanel(vm = vm) { vm.activePanel = null }
                    "chroma" -> ChromaPanel(vm = vm) { vm.activePanel = null }
                    "motionblur" -> MotionBlurPanel(vm = vm) { vm.activePanel = null }
                    "particles" -> ParticlePanel(vm = vm) { vm.activePanel = null }
                    "lens" -> LensPanel(vm = vm) { vm.activePanel = null }
                    "film" -> FilmPanel(vm = vm) { vm.activePanel = null }
                    "template" -> TemplatePanel(vm = vm, onApply = { vm.showToast("已套用: $it") }) {
                        vm.activePanel = null
                    }
                }
            }
        }
    }
}
