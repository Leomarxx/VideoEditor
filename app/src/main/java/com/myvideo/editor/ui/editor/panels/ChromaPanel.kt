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

@Composable
fun ChromaPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var bgColor by remember { mutableStateOf(Color(0xFF00FF00)) }
    var bgHex by remember { mutableStateOf("#00FF00") }
    var bgType by remember { mutableStateOf("透明") }
    var screenType by remember { mutableStateOf("绿幕") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("吸色选取", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(bgColor)
                .border(2.dp, CG.Line2, RoundedCornerShape(8.dp))
                .clickable {
                    val colors = listOf(Color(0xFF00FF00), Color(0xFF00B300), Color(0xFF009900),
                        Color(0xFF0000FF), Color(0xFF0066CC), Color(0xFFFF00FF),
                        Color.White, Color.Black)
                    val idx = (colors.indexOf(bgColor) + 1) % colors.size
                    bgColor = colors[idx]
                    bgHex = listOf("#00FF00", "#00B300", "#009900", "#0000FF", "#0066CC", "#FF00FF", "#FFFFFF", "#000000")[idx]
                })
            Column {
                Text("点击吸管选取背景色", fontSize = 10.sp, color = CG.T2)
                Text(bgHex, fontSize = 8.sp, color = CG.T3, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("抠像参数", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("容差", 0, 30, 100)
        CgSlider("边缘羽化", 0, 10, 50)
        CgSlider("去溢色", 0, 0, 100)
        CgSlider("收缩", 0, 0, 20)
        Spacer(modifier = Modifier.height(14.dp))

        Text("高级抠像", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("暗部抑制", 0, 0, 50)
        CgSlider("亮部抑制", 0, 0, 50)
        CgSlider("边缘检测", 0, 0, 100)
        Spacer(modifier = Modifier.height(14.dp))

        Text("替换背景", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("透明", "纯色", "图片", "视频").forEach { t ->
                OptionChip(t, bgType == t) { bgType = t }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("颜色选择", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("绿幕", "蓝幕", "自定义").forEach { t ->
                OptionChip(t, screenType == t) { screenType = t }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用抠像") { onClose() }
    }
}
