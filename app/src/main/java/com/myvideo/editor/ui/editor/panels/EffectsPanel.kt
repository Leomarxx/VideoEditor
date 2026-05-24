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
import com.myvideo.editor.engine.VideoFilterEngine
import com.myvideo.editor.ui.editor.EditorViewModel

@Composable
fun EffectsPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    val filterEngine = remember { VideoFilterEngine(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 预设滤镜
        Text("预设滤镜", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        var selectedPreset by remember { mutableStateOf<String?>(null) }
        val presets = filterEngine.getPresetNames()
        presets.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { preset ->
                    MultiChip(preset, selectedPreset == preset) {
                        selectedPreset = if (selectedPreset == preset) null else preset
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 基础效果
        Text("基础", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        var basicSel by remember { mutableStateOf(setOf<String>()) }
        listOf("模糊", "锐化", "发光", "阴影", "浮雕", "马赛克", "像素化", "色差").chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { f ->
                    MultiChip(f, basicSel.contains(f)) {
                        basicSel = if (basicSel.contains(f)) basicSel - f else basicSel + f
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 风格化
        Text("风格化", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        var styleSel by remember { mutableStateOf(setOf<String>()) }
        listOf("老电影", "故障", "复古", "霓虹", "油画", "素描", "卡通", "水墨").chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { f ->
                    MultiChip(f, styleSel.contains(f)) {
                        styleSel = if (styleSel.contains(f)) styleSel - f else styleSel + f
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 变形
        Text("变形", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        var deformSel by remember { mutableStateOf(setOf<String>()) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("扭曲", "波纹", "球面化", "湍流置换").forEach { f ->
                MultiChip(f, deformSel.contains(f)) {
                    deformSel = if (deformSel.contains(f)) deformSel - f else deformSel + f
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        ApplyButton("添加到片段") {
            val clip = vm.selectedClip()
            if (clip != null) {
                val effects = (basicSel + styleSel + deformSel).toList()
                val presetName = selectedPreset ?: ""
                vm.showToast("已添加: ${effects.size}个效果${if (presetName.isNotEmpty()) " + $presetName" else ""}")
            }
            onClose()
        }
    }
}

@Composable
private fun MultiChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) CG.AccS else CG.Card)
        .weight(1f)
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (selected) CG.AccL else CG.T2)
    }
}
