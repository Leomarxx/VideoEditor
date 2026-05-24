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
fun PiPPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var animation by remember { mutableStateOf("淡入") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 预览区
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp))
            .background(Color.Black), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.4f).fillMaxHeight(0.4f)
                .border(2.dp, CG.Acc.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .background(CG.Acc.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text("拖拽移动", fontSize = 8.sp, color = CG.AccL)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("位置", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("X", 0, 20, 100)
        CgSlider("Y", 0, 20, 100)
        Spacer(modifier = Modifier.height(14.dp))

        Text("大小", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("缩放", 10, 40, 200)
        Spacer(modifier = Modifier.height(14.dp))

        Text("旋转", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("角度", -180, 0, 180)
        Spacer(modifier = Modifier.height(14.dp))

        Text("样式", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        CgSlider("圆角", 0, 4, 50)
        CgSlider("边框", 0, 0, 5)
        CgSlider("阴影", 0, 0, 30)
        Spacer(modifier = Modifier.height(14.dp))

        Text("动画", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("无", "淡入", "弹入", "缩放入").forEach { a ->
                OptionChip(a, animation == a) { animation = a }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用画中画") { onClose() }
    }
}
