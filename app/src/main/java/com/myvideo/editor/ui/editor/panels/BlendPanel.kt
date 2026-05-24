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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.ui.editor.EditorViewModel

@Composable
fun BlendPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    var mode by remember { mutableStateOf("正常") }
    var opacity by remember { mutableFloatStateOf(100f) }
    var fillOpacity by remember { mutableFloatStateOf(100f) }
    var invertMask by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 混合模式
        Text("混合模式", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val modes = listOf("正常", "正片叠底", "滤色", "叠加", "柔光", "强光", "差值", "排除", "色相", "饱和度", "颜色", "亮度")
        modes.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { m -> OptionChip(m, mode == m) { mode = m } }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 不透明度
        Text("不透明度: ${"%.0f".format(opacity)}%", fontSize = 10.sp, color = CG.T2)
        Spacer(modifier = Modifier.height(4.dp))
        CgSlider("不透明度", 0, opacity.toInt(), 100)
        Spacer(modifier = Modifier.height(8.dp))

        // 填充不透明度
        Text("填充不透明度: ${"%.0f".format(fillOpacity)}%", fontSize = 10.sp, color = CG.T2)
        Spacer(modifier = Modifier.height(4.dp))
        CgSlider("填充", 0, fillOpacity.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        // 遮罩选项
        Text("遮罩", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("反转遮罩", invertMask) { invertMask = !invertMask }
            OptionChip("添加遮罩", false) { vm.showToast("已添加遮罩") }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 预览
        Text("预览", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(Color(0xFF4A90D9)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF4A90D9).copy(alpha = opacity / 100f)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("原始", fontSize = 9.sp, color = CG.T3)
            Text("混合后", fontSize = 9.sp, color = CG.T3)
        }

        Spacer(modifier = Modifier.height(14.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) {
                val idx = vm.clips.indexOf(clip)
                if (idx >= 0) {
                    vm.showToast("已应用: $mode ${"%.0f".format(opacity)}%")
                }
            } else { vm.showToast("请先选择片段") }
            onClose()
        }
    }
}
