package com.myvideo.editor.ui.color

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
fun ColorGradingPresetsPanel(
    onApply: (String) -> Unit = {}
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val presets = listOf(
        "电影" to Color(0xFF4A6FA5),
        "复古" to Color(0xFF8B6914),
        "清冷" to Color(0xFF5B8FA8),
        "暖阳" to Color(0xFFC4813D),
        "黑白" to Color(0xFF888888),
        "赛博" to Color(0xFF6E3BFF),
        "日系" to Color(0xFFB8A88A),
        "胶片" to Color(0xFF7A8B5A),
        "橙蓝" to Color(0xFFD4793A),
        "青橙" to Color(0xFF3A8B8B)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("调色预设", fontSize = 11.sp, color = Color(0xFF999999), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        presets.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (name, color) ->
                    val sel = name == selected
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (sel) Color(0xFF2A2A2A) else Color(0xFF161616))
                            .clickable { selected = name; onApply(name) }
                            .padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(color))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(name, fontSize = 9.sp, color = if (sel) Color.White else Color(0xFF999999))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
