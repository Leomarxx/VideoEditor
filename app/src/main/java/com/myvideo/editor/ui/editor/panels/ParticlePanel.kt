package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParticlePanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var type by remember { mutableStateOf("雪花") }
    var color by remember { mutableStateOf(Color.White) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 粒子预览
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                for (i in 0 until 30) {
                    val x = (Math.random() * w).toFloat()
                    val y = (Math.random() * h).toFloat()
                    val r = (Math.random() * 2 + 1).toFloat()
                    drawCircle(Color.White.copy(alpha = 0.6f), radius = r, center = Offset(x, y))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("类型", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("雪花", "火花", "光斑", "灰尘", "烟雾", "泡泡").forEach { t ->
                OptionChip(t, type == t) { type = t }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        CgSlider("数量", 10, 50, 200)
        CgSlider("速度", 1, 5, 20)
        CgSlider("大小", 1, 4, 15)
        Spacer(modifier = Modifier.height(14.dp))
        Text("颜色", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Color.White, CG.Gold, CG.Acc, CG.Red).forEach { c ->
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(c)
                    .then(if (color == c) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { color = c })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("添加粒子") { onClose() }
    }
}
