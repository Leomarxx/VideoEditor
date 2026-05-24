package com.myvideo.editor.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        // 顶栏
        Row(modifier = Modifier.fillMaxWidth().height(48.dp)
            .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)).clickable { onBack() },
                contentAlignment = Alignment.Center) {
                Text("←", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("隐私政策", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        // WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = true
                    settings.defaultTextEncodingName = "UTF-8"
                    loadUrl("file:///android_asset/privacy_policy.html")
                }
            }
        )
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp)
            .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)).clickable { onBack() },
                contentAlignment = Alignment.Center) {
                Text("←", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("用户协议", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "UTF-8"
                    loadUrl("file:///android_asset/terms_of_service.html")
                }
            }
        )
    }
}
