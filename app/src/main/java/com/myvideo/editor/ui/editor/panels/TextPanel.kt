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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var text by remember { mutableStateOf("双击编辑文字") }
    var font by remember { mutableStateOf("默认") }
    var animation by remember { mutableStateOf("淡入") }
    val colors = listOf("#ffffff", "#ff4444", "#44ff44", "#4444ff", "#ffff44", "#ff44ff", "#44ffff", "#e8a820", "#4a90d9", "#000000")

    Column(modifier = Modifier.fillMaxWidth()) {
        // 预览区
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp))
            .background(CG.Card).border(1.dp, CG.Line), contentAlignment = Alignment.Center) {
            Text(text.ifEmpty { "双击编辑文字" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 内容输入
        Text("内容", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp))
            .background(CG.Card).border(1.dp, CG.Line2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.CenterStart) {
            Text(text, fontSize = 12.sp, color = CG.T1, modifier = Modifier.padding(horizontal = 10.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 字体
        Text("字体", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("默认", "粗体", "细体", "手写").forEach { f -> OptionChip(f, font == f) { font = f } }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 大小
        CgSlider("大小", 8, 20, 72)
        Spacer(modifier = Modifier.height(14.dp))

        // 颜色
        Text("颜色", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        var selectedColor by remember { mutableStateOf(colors[0]) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEach { c ->
                val parsed = Color(android.graphics.Color.parseColor(c))
                Box(modifier = Modifier.size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parsed)
                    .then(if (selectedColor == c) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { selectedColor = c })
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 描边
        CgSlider("描边", 0, 0, 5)
        Spacer(modifier = Modifier.height(14.dp))

        // 阴影
        Text("阴影", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("距离", 0, 0, 20); CgSlider("模糊", 0, 0, 20)
        Spacer(modifier = Modifier.height(14.dp))

        // 动画
        Text("动画", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("无", "淡入", "弹出", "逐字", "打字机").forEach { a -> OptionChip(a, animation == a) { animation = a } }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) {
                vm.showToast("已应用文字: $text")
            } else {
                vm.showToast("请先选择片段")
            }
            onClose()
        }
    }
}
