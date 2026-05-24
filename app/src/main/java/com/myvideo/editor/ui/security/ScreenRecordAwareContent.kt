package com.myvideo.editor.ui.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ScreenRecordAwareContent(
    protectScreen: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(protectScreen) {
        if (protectScreen && context is Activity) {
            context.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    val isRecording = remember { mutableStateOf(ScreenRecordDetector.isScreenRecording(context)) }

    content()
}
