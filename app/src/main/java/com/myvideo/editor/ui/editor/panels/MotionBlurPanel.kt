package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.ui.editor.EditorViewModel

@Composable
fun MotionBlurPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    var strength by remember { mutableFloatStateOf(50f) }
    var angle by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableStateOf("水平") }
    var quality by remember { mutableStateOf("高") }
    var useKeyframes by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("强度: ${"%.0f".format(strength)}%", fontSize = 10.sp, color = CG.T2)
        Spacer(modifier = Modifier.height(4.dp))
        CgSlider("强度", 0, strength.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("角度: ${"%.0f".format(angle)}°", fontSize = 10.sp, color = CG.T2)
        Spacer(modifier = Modifier.height(4.dp))
        CgSlider("角度", 0, angle.toInt(), 360)
        Spacer(modifier = Modifier.height(10.dp))

        Text("方向", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("水平", "垂直", "自定义").forEach { d ->
                OptionChip(d, direction == d) { direction = d }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text("质量", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("低", "中", "高").forEach { q ->
                OptionChip(q, quality == q) { quality = q }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text("关键帧", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OptionChip("启用关键帧", useKeyframes) { useKeyframes = !useKeyframes }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) {
                vm.showToast("动态模糊: ${"%.0f".format(strength)}% $direction")
            } else { vm.showToast("请先选择片段") }
            onClose()
        }
    }
}
