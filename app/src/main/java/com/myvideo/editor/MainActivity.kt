package com.myvideo.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.security.ComplianceAuditor
import com.myvideo.editor.security.SecurityReporter
import com.myvideo.editor.startup.SecurityInitRunner

/**
 * NexClip 主Activity
 * 编号59：隐私弹窗+安全教育
 * 编号10：界面保护
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                NexClipApp(this)
            }
        }
    }
}

@Composable
fun NexClipApp(activity: ComponentActivity) {
    var privacyAccepted by remember { mutableStateOf(ComplianceAuditor.isPrivacyAccepted(activity)) }
    var securityResult by remember { mutableStateOf<String?>(null) }
    var showHome by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        when {
            // 编号59：隐私弹窗（首次使用必须同意）
            !privacyAccepted -> {
                PrivacyScreen(
                    onAccept = {
                        privacyAccepted = true
                        // 安全初始化
                        SecurityInitRunner.init(activity) { result ->
                            securityResult = result.message
                            showHome = true
                        }
                    },
                    onReject = {
                        activity.finish()
                    }
                )
            }
            // 安全初始化中
            securityResult == null -> {
                LoadingScreen()
            }
            // 主界面
            showHome -> {
                HomeScreen(securityResult ?: "")
            }
        }
    }
}

@Composable
fun PrivacyScreen(onAccept: () -> Unit, onReject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "用户协议和隐私政策",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用NexClip！\n\n" +
                    "我们非常重视您的个人信息保护。在使用前，请您阅读并同意我们的《用户协议》和《隐私政策》。\n\n" +
                    "我们仅收集必要的设备信息和使用数据，用于提供和改进服务。您可随时撤回授权、删除数据或导出数据。\n\n" +
                    "未经您同意，我们不会从第三方获取、共享或向其提供您的信息。",
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("同意并继续", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onReject) {
            Text("拒绝并退出", fontSize = 14.sp, color = Color(0xFF888888))
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在初始化安全模块...",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
fun HomeScreen(securityMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NexClip",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI视频编辑器",
            fontSize = 16.sp,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 安全状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "安全状态",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = securityMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // TODO: 视频编辑功能入口
        Text(
            text = "视频编辑功能开发中...",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}
