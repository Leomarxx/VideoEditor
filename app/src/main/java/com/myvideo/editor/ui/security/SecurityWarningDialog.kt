package com.myvideo.editor.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecurityWarningDialog(
    title: String = "安全警告",
    message: String = "",
    severity: String = "WARN",
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    val color = when (severity) {
        "CRITICAL" -> Color(0xFFE85050)
        "HIGH" -> Color(0xFFE8A820)
        else -> Color(0xFF4A90D9)
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
        contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(300.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, fontSize = 13.sp, color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A2A))
                    .clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Text("忽略", fontSize = 13.sp, color = Color(0xFF999999))
                }
                Box(modifier = Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(8.dp)).background(color)
                    .clickable { onConfirm() }, contentAlignment = Alignment.Center) {
                    Text("确定", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(onClick = onClick)
)
