package com.myvideo.editor.ui.editor.panels

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.engine.VideoEngine
import com.myvideo.editor.ui.editor.EditorViewModel
import java.io.File

@Composable
fun ExportPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    val context = LocalContext.current
    var preset by remember { mutableStateOf("自定义") }
    var resolution by remember { mutableStateOf("1080P") }
    var format by remember { mutableStateOf("MP4") }
    var customRatio by remember { mutableStateOf(false) }
    var customW by remember { mutableStateOf("1920") }
    var customH by remember { mutableStateOf("1080") }

    // 解析分辨率
    val (exportW, exportH) = when {
        customRatio -> (customW.toIntOrNull() ?: 1920) to (customH.toIntOrNull() ?: 1080)
        resolution == "720P" -> 1280 to 720
        resolution == "1080P" -> 1920 to 1080
        resolution == "2K" -> 2560 to 1440
        resolution == "4K" -> 3840 to 2160
        else -> 1920 to 1080
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("预设", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("自定义", "抖音", "朋友圈", "YouTube", "Instagram").forEach { p ->
                OptionChip(p, preset == p) { preset = p }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("分辨率", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("720P", "1080P", "2K", "4K").forEach { r ->
                OptionChip(r, resolution == r && !customRatio) {
                    resolution = r; customRatio = false
                }
            }
            OptionChip("自定义", customRatio) { customRatio = true }
        }
        if (customRatio) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(CG.Card).border(1.dp, CG.Line2, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart) {
                    Text("  W: $customW", fontSize = 11.sp, color = CG.T1, fontFamily = FontFamily.Monospace)
                }
                Text("×", fontSize = 12.sp, color = CG.T3)
                Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(CG.Card).border(1.dp, CG.Line2, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart) {
                    Text("  H: $customH", fontSize = 11.sp, color = CG.T1, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        CgSlider("码率", 1, 20, 100)
        CgSlider("帧率", 15, 30, 60)
        Spacer(modifier = Modifier.height(14.dp))

        Text("格式", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("MP4", "MOV", "GIF").forEach { f -> OptionChip(f, format == f) { format = f } }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 进度条（连接真实引擎）
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CG.Card)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(vm.exportProgress / 100f)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(CG.Acc, CG.AccL))))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            when {
                vm.exportDone -> "导出完成！"
                vm.exportError != null -> "错误: ${vm.exportError}"
                vm.isExporting -> "导出中 ${vm.exportProgress.toInt()}%"
                else -> "准备导出"
            },
            fontSize = 10.sp, color = CG.T3, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 导出按钮（连接VideoEngine）
        ApplyButton(
            if (vm.exportDone) "完成"
            else if (vm.isExporting) "导出中..."
            else "开始导出"
        ) {
            if (!vm.isExporting && !vm.exportDone) {
                // 获取第一个有URI的片段
                val firstUri = vm.videoUris.values.firstOrNull()
                if (firstUri != null) {
                    vm.isExporting = true
                    vm.exportProgress = 0f
                    vm.exportError = null

                    val engine = VideoEngine(context)
                    val outputDir = File(context.getExternalFilesDir(
                        Environment.DIRECTORY_MOVIES), "NexClip")
                    if (!outputDir.exists()) outputDir.mkdirs()
                    val outputPath = File(outputDir,
                        "export_${System.currentTimeMillis()}.mp4").absolutePath

                    val config = VideoEngine.ExportConfig(
                        outputPath = outputPath,
                        width = exportW,
                        height = exportH,
                        fps = 30,
                        bitrate = 8_000_000
                    )

                    engine.exportVideo(Uri.parse(firstUri), config,
                        object : VideoEngine.ProgressCallback {
                            override fun onProgress(percent: Float) {
                                vm.exportProgress = percent
                            }
                            override fun onComplete(path: String) {
                                vm.exportDone = true
                                vm.isExporting = false
                            }
                            override fun onError(error: String) {
                                vm.exportError = error
                                vm.isExporting = false
                            }
                        }
                    )
                }
            }
        }
    }
}
