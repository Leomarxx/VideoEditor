package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransitionPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var selected by remember { mutableStateOf("淡入淡出") }
    var easing by remember { mutableStateOf("线性") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("转场类型", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val transitions = listOf("淡入淡出", "滑动左", "滑动右", "擦除", "缩放", "旋转", "百叶窗", "棋盘格", "径向", "交叉溶解", "闪白", "抖动")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            transitions.take(4).forEach { t -> OptionChip(t, selected == t) { selected = t } }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            transitions.drop(4).take(4).forEach { t -> OptionChip(t, selected == t) { selected = t } }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            transitions.drop(8).forEach { t -> OptionChip(t, selected == t) { selected = t } }
        }
        Spacer(modifier = Modifier.height(14.dp))
        CgSlider("时长(ms)", 100, 500, 2000)
        Spacer(modifier = Modifier.height(14.dp))
        Text("缓动", fontSize = 9.sp, color = CG.T4, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("线性", "缓入", "缓出", "弹性").forEach { e -> OptionChip(e, easing == e) { easing = e } }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) vm.showToast("已应用转场: $selected") else vm.showToast("请先选择片段")
            onClose()
        }
    }
}
