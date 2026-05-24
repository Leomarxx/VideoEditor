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
fun HSLSecondaryPanel(
    onColorSelected: (String) -> Unit = {}
) {
    var selectedChannel by remember { mutableStateOf("红") }
    val channels = listOf("红", "橙", "黄", "绿", "青", "蓝", "紫", "品红")
    val channelColors = listOf(
        Color(0xFFFF4444), Color(0xFFFF8844), Color(0xFFFFCC00),
        Color(0xFF44CC44), Color(0xFF44CCCC), Color(0xFF4488FF),
        Color(0xFF8844FF), Color(0xFFFF44CC)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("HSL 次级调色", fontSize = 11.sp, color = Color(0xFF999999), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        channels.forEachIndexed { idx, ch ->
            val sel = ch == selectedChannel
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                .background(if (sel) Color(0xFF1A2A3A) else Color.Transparent)
                .clickable { selectedChannel = ch }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp))
                    .background(channelColors[idx]))
                Spacer(modifier = Modifier.width(8.dp))
                Text(ch, fontSize = 12.sp, color = if (sel) Color.White else Color(0xFFCCCCCC))
            }
        }
    }
}
