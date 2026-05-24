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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrackingPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var mode by remember { mutableStateOf("位置") }
    var applyTo by remember { mutableStateOf("位置X/Y") }
    var tracking by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    var pointCount by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("追踪模式", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("位置", "位置+缩放", "位置+旋转", "仿射").forEach { m ->
                OptionChip(m, mode == m) { mode = m }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("追踪点", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        // 追踪点列表
        if (pointCount > 0) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                repeat(pointCount) { i ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(CG.Green))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("追踪点 ${i + 1}", fontSize = 10.sp, color = CG.T2)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp))
            .background(CG.Card).border(1.dp, CG.Line2, RoundedCornerShape(8.dp))
            .clickable { pointCount++ }, contentAlignment = Alignment.Center) {
            Text("添加追踪点", fontSize = 11.sp, color = CG.T1)
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("搜索区域", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("搜索范围", 20, 80, 200)
        Spacer(modifier = Modifier.height(14.dp))

        Text("应用到", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("位置X/Y", "缩放", "旋转", "文字/贴纸").forEach { a ->
                OptionChip(a, applyTo == a) { applyTo = a }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 进度条
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CG.Card)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress / 100f).clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(CG.Acc, CG.AccL))))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            when {
                done -> "追踪完成，已生成 $pointCount 个关键帧"
                tracking -> "追踪中 ${progress.toInt()}%"
                else -> "准备追踪"
            },
            fontSize = 10.sp, color = CG.T3, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        ApplyButton(if (tracking) "追踪中..." else "开始追踪") {
            if (!tracking && pointCount > 0) {
                tracking = true
                Thread {
                    var p = 0f
                    while (p < 100) {
                        p += (3..8).random()
                        if (p > 100) p = 100f
                        progress = p
                        Thread.sleep(200)
                    }
                    done = true
                    tracking = false
                }.start()
            }
        }
    }
}

@Composable
private fun BorderBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.border(1.dp, CG.Line2, RoundedCornerShape(8.dp))) { content() }
}
