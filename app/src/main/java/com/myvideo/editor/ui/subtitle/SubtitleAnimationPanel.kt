package com.myvideo.editor.ui.subtitle

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

@Composable
fun SubtitleAnimationPanel(onApply: (String) -> Unit = {}) {
    var selected by remember { mutableStateOf("淡入") }
    val animations = listOf("淡入", "淡出", "弹出", "逐字", "打字机", "滑入", "渐显", "闪烁")
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("字幕动画", fontSize = 11.sp, color = Color(0xFF999999), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        animations.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { anim ->
                    val sel = anim == selected
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFF4A90D9).copy(0.2f) else Color(0xFF1A1A1A))
                        .clickable { selected = anim }.padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center) {
                        Text(anim, fontSize = 10.sp, color = if (sel) Color(0xFF6AAFE6) else Color(0xFF999999))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
