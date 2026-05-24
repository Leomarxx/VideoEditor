package com.myvideo.editor.ui.editor.panels

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.ui.editor.EditorViewModel

@Composable
fun SpeedPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    val clip = vm.selectedClip()
    var speed by remember { mutableStateOf(clip?.speed?.toString() ?: "1.00") }
    var reversed by remember { mutableStateOf(clip?.isReversed ?: false) }
    var curve by remember { mutableStateOf("匀速") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("${speed}x", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CG.AccL,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(8.dp, 8.dp, 0.dp, 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        val speeds = listOf("0.25", "0.5", "0.75", "1.0", "1.5", "2.0", "3.0", "4.0")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            speeds.forEach { s ->
                OptionChip("${s}x", speed == s) { speed = s }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        CgSlider("自定义", 10, (speed.toFloatOrNull()?.times(100))?.toInt()?.coerceIn(10, 400) ?: 100, 400)
        Spacer(modifier = Modifier.height(14.dp))

        Text("倒放", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        ToggleSwitch(reversed) { reversed = it }
        Spacer(modifier = Modifier.height(14.dp))

        Text("速度曲线", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("匀速", "缓入", "缓出", "缓入缓出").forEach { c ->
                OptionChip(c, curve == c) { curve = c }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            // 连接EditorViewModel：应用速度到选中片段
            clip?.let { c ->
                val idx = vm.clips.indexOf(c)
                if (idx >= 0) {
                    val newSpeed = speed.toFloatOrNull() ?: 1f
                    vm.clips[idx] = c.copy(speed = newSpeed, isReversed = reversed)
                    vm.showToast("已应用: ${speed}x ${if (reversed) "倒放" else ""}")
                }
            }
            onClose()
        }
    }
}

@Composable
private fun ToggleSwitch(isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Box(modifier = Modifier.width(40.dp).height(22.dp)
        .clip(RoundedCornerShape(11.dp))
        .background(if (isOn) CG.Acc else CG.Line)
        .clickable { onToggle(!isOn) },
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(modifier = Modifier.padding(2.dp).size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White))
    }
}
