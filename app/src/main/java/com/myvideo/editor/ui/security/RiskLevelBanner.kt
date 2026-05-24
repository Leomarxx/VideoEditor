package com.myvideo.editor.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RiskLevel(val label: String, val color: Color) {
    SAFE("安全", Color(0xFF6EC850)),
    LOW("低风险", Color(0xFF4A90D9)),
    MEDIUM("中风险", Color(0xFFE8A820)),
    HIGH("高风险", Color(0xFFE85050)),
    CRITICAL("严重", Color(0xFFFF0000))
}

@Composable
fun RiskLevelBanner(level: RiskLevel, detail: String = "") {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(level.color.copy(alpha = 0.12f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
            .background(level.color))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(level.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = level.color)
            if (detail.isNotEmpty()) {
                Text(detail, fontSize = 10.sp, color = Color(0xFF999999))
            }
        }
    }
}
