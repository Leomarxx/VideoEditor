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
fun PaywallOverlay(onSubscribe: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    var selectedPlan by remember { mutableStateOf("year") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC080808)),
        contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(320.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Text("NexClip PRO", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("解锁全部高级功能", fontSize = 13.sp, color = Color(0xFF999999))

            Spacer(modifier = Modifier.height(20.dp))

            val features = listOf("无水印导出", "4K分辨率", "AI智能字幕", "AI智能抠图", "全部滤镜特效", "无限项目")
            features.forEach { feature ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", fontSize = 13.sp, color = Color(0xFF6EC850))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, fontSize = 13.sp, color = Color(0xFFCCCCCC))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PlanOption("月付", "¥29/月", "", selectedPlan == "month") { selectedPlan = "month" }
            Spacer(modifier = Modifier.height(8.dp))
            PlanOption("季付", "¥76/季", "省13%", selectedPlan == "quarter") { selectedPlan = "quarter" }
            Spacer(modifier = Modifier.height(8.dp))
            PlanOption("年付", "¥228/年", "省35% · 推荐", selectedPlan == "year", highlighted = true) { selectedPlan = "year" }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE8A820), Color(0xFFD49420))))
                .clickable { onSubscribe() }, contentAlignment = Alignment.Center) {
                Text("立即订阅", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("会员可随时取消自动续费，虚拟商品不支持无理由退款", fontSize = 10.sp, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(8.dp))
            Text("暂不升级", fontSize = 12.sp, color = Color(0xFF444444),
                modifier = Modifier.clickable { onDismiss() })
        }
    }
}

@Composable
private fun PlanOption(name: String, price: String, tag: String, selected: Boolean, highlighted: Boolean = false, onClick: () -> Unit) {
    val bgColor = when {
        selected && highlighted -> Color(0xFFE8A820).copy(alpha = 0.15f)
        selected -> Color(0xFF4A90D9).copy(alpha = 0.15f)
        else -> Color(0xFF222222)
    }
    val borderColor = when {
        selected && highlighted -> Color(0xFFE8A820).copy(alpha = 0.5f)
        selected -> Color(0xFF4A90D9).copy(alpha = 0.5f)
        else -> Color(0xFF333333)
    }

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(bgColor).border(1.dp, borderColor, RoundedCornerShape(10.dp))
        .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (tag.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tag, fontSize = 9.sp, color = if (highlighted) Color(0xFFE8A820) else Color(0xFF6EC850),
                        modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(if (highlighted) Color(0xFFE8A820).copy(0.12f) else Color(0xFF6EC850).copy(0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
        }
        Text(price, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (highlighted) Color(0xFFE8A820) else Color(0xFFCCCCCC))
    }
}
