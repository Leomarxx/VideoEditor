package com.myvideo.editor.startup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(onReady: () -> Unit = {}) {
    val context = LocalContext.current
    val vm = remember { SplashViewModel() }

    LaunchedEffect(Unit) { vm.startLoading(context) }
    LaunchedEffect(vm.isReady) { if (vm.isReady) onReady() }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, animationSpec = tween(800)) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080808)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha.value)) {
            Box(modifier = Modifier.size(100.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Text("N", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("NexClip", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("AI视频编辑器", fontSize = 13.sp, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(40.dp))
            Text(vm.loadMessage, fontSize = 11.sp, color = Color(0xFF444444))
            if (vm.deviceInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(vm.deviceInfo, fontSize = 10.sp, color = Color(0xFF555555), textAlign = TextAlign.Center)
            }
            if (vm.modelStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(vm.modelStatus, fontSize = 9.sp, color = Color(0xFF333333), textAlign = TextAlign.Center)
            }
        }
    }
}
