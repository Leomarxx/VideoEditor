package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var selectedAudio by remember { mutableStateOf<String?>(null) }
    var effect by remember { mutableStateOf("无") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("音频素材", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val audios = listOf(
            "背景音乐 01" to "0:15", "背景音乐 02" to "0:23",
            "环境音" to "0:31", "鼓点节奏" to "0:39",
            "钢琴旋律" to "0:47", "电子氛围" to "0:55"
        )
        audios.forEach { (name, dur) ->
            val sel = selectedAudio == name
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (sel) CG.AccS else CG.Card)
                .then(if (sel) Modifier.border(1.dp, CG.Acc, RoundedCornerShape(8.dp)) else Modifier)
                .clickable { selectedAudio = name }
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("▶", fontSize = 12.sp, color = CG.T3)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 11.sp, color = CG.T1, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    Text("$dur · 免费", fontSize = 8.sp, color = CG.T3)
                }
                Text("使用", fontSize = 8.sp, color = CG.AccL, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(CG.AccS).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text("音量", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        CgSlider("主音量", 0, 80, 150)
        CgSlider("淡入", 0, 0, 2000)
        CgSlider("淡出", 0, 0, 2000)
        Spacer(modifier = Modifier.height(14.dp))
        Text("音效", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("无", "混响", "回声").forEach { e -> OptionChip(e, effect == e) { effect = e } }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("均衡器", "降噪", "变速不变调").forEach { e -> OptionChip(e, effect == e) { effect = e } }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("添加到时间轴") { onClose() }
    }
}
