package com.myvideo.editor.ui.membership

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MemberGateDialog(
    featureName: String = "此功能",
    onSubscribe: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
        contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(300.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PRO", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFE8A820),
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8A820).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("解锁$featureName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("升级到 PRO 版本即可解锁全部高级功能，包括无水印导出、4K分辨率、AI功能等。",
                fontSize = 13.sp, color = Color(0xFF999999), textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // PRO价格
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE8A820).copy(0.2f), Color(0xFFE8A820).copy(0.08f))))
                .border(1.dp, Color(0xFFE8A820).copy(0.3f), RoundedCornerShape(12.dp))
                .clickable { onSubscribe() }.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¥228/年", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8A820))
                Spacer(modifier = Modifier.height(4.dp))
                Text("每天不到 ¥0.63 · 月付¥29 · 季付¥76", fontSize = 11.sp, color = Color(0xFF999999))
                Text("会员可随时取消自动续费，虚拟商品不支持无理由退款", fontSize = 9.sp, color = Color(0xFF666666))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("暂不升级", fontSize = 13.sp, color = Color(0xFF666666),
                modifier = Modifier.clickable { onDismiss() })
        }
    }
}
