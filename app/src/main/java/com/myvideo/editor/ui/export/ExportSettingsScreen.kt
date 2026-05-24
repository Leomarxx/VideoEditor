package com.myvideo.editor.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExportSettingsScreen(onBack: () -> Unit = {}) {
    var resolution by remember { mutableStateOf("1080p") }
    var fps by remember { mutableStateOf("30") }
    var bitrate by remember { mutableStateOf("8M") }
    var codec by remember { mutableStateOf("H.264") }
    var format by remember { mutableStateOf("MP4") }
    var hdr by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080808)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("导出设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingSection("分辨率") { ChipGroup(listOf("720p","1080p","2K","4K"), resolution) { resolution = it } }
        SettingSection("帧率") { ChipGroup(listOf("24","30","60"), fps) { fps = it } }
        SettingSection("码率") { ChipGroup(listOf("4M","8M","16M","32M"), bitrate) { bitrate = it } }
        SettingSection("编码器") { ChipGroup(listOf("H.264","H.265","AV1"), codec) { codec = it } }
        SettingSection("格式") { ChipGroup(listOf("MP4","MOV","WebM"), format) { format = it } }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).clickable { hdr = !hdr }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("HDR 导出", fontSize = 13.sp, color = Color(0xFFCCCCCC))
            Text(if (hdr) "ON" else "OFF", fontSize = 12.sp, color = if (hdr) Color(0xFF6EC850) else Color(0xFF666666), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("预估文件大小: ~${estimateSize(resolution, fps, bitrate)}MB/分钟", fontSize = 10.sp, color = Color(0xFF666666), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(title, fontSize = 11.sp, color = Color(0xFF666666), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val sel = opt == selected
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) Color(0xFF4A90D9).copy(0.2f) else Color(0xFF1A1A1A)).clickable { onSelect(opt) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(opt, fontSize = 12.sp, color = if (sel) Color(0xFF6AAFE6) else Color(0xFF999999), fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

private fun estimateSize(res: String, fps: String, br: String): String {
    val base = when (res) { "720p" -> 30; "1080p" -> 60; "2K" -> 120; "4K" -> 250; else -> 60 }
    val fpsMul = fps.toFloatOrNull()?.div(30f) ?: 1f
    val brMul = br.replace("M","").toFloatOrNull()?.div(8f) ?: 1f
    return "%.0f".format(base * fpsMul * brMul)
}
