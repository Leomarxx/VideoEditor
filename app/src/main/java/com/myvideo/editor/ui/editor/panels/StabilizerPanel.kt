package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StabilizerPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var mode by remember { mutableStateOf("平滑") }
    var range by remember { mutableStateOf("位置") }
    var stabilizing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var done by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("稳定模式", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("平滑", "锁定", "自定义").forEach { m -> OptionChip(m, mode == m) { mode = m } }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("分析范围", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("位置", "位置+旋转", "透视").forEach { r -> OptionChip(r, range == r) { range = r } }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("平滑参数", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("平滑度", 0, 50, 100)
        CgSlider("裁切限制", 0, 10, 50)
        CgSlider("缩放补偿", 100, 105, 130)
        Spacer(modifier = Modifier.height(14.dp))

        Text("高级", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("去果冻", 0, 0, 100)
        CgSlider("旋转稳定", 0, 0, 100)
        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CG.Card)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress / 100f).clip(RoundedCornerShape(3.dp))
                .background(Brush.linearGradient(listOf(CG.Acc, CG.AccL))))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(when { done -> "稳定完成"; stabilizing -> "分析中 ${progress.toInt()}%"; else -> "准备分析" },
            fontSize = 10.sp, color = CG.T3, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))

        ApplyButton(if (stabilizing) "稳定中..." else "开始稳定") {
            if (!stabilizing) {
                stabilizing = true
                Thread {
                    val phases = listOf("分析抖动轨迹", "计算补偿矩阵", "平滑运动曲线", "应用裁切补偿", "优化边缘")
                    var phase = 0; var p = 0f
                    while (p < 100) {
                        p += (2..6).random()
                        if (p >= 20 * (phase + 1) && phase < phases.lastIndex) phase++
                        if (p > 100) p = 100f
                        progress = p; Thread.sleep(250)
                    }
                    done = true; stabilizing = false
                }.start()
            }
        }
    }
}
