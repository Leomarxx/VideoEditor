package com.myvideo.editor.ui.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(
    onPrivacyPolicy: () -> Unit = {},
    onTerms: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }
    val versionCode = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (e: Exception) { 1L }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))
        .padding(horizontal = 24.dp)) {
        // 顶栏
        Row(modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)).clickable { onBack() },
                contentAlignment = Alignment.Center) {
                Text("←", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("关于", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(60.dp))

        // APP图标占位
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center) {
            Text("N", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("NexClip", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = Color.White, modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
        Text("AI视频编辑器", fontSize = 13.sp, color = Color(0xFF888888),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(8.dp))
        Text("版本 $versionName ($versionCode)", fontSize = 11.sp,
            color = Color(0xFF666666), modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        // 链接列表
        LinkItem("隐私政策") { onPrivacyPolicy() }
        LinkItem("用户协议") { onTerms() }

        Spacer(modifier = Modifier.height(40.dp))

        Text("© 2025 NexClip. All rights reserved.",
            fontSize = 10.sp, color = Color(0xFF444444),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun LinkItem(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp)
        .clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A))
        .clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color(0xFFCCCCCC))
        Text(">", fontSize = 14.sp, color = Color(0xFF444444))
    }
    Spacer(modifier = Modifier.height(8.dp))
}
