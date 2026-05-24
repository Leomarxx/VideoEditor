package com.myvideo.editor.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PhoneLandscapeLayout(
    preview: @Composable () -> Unit,
    timeline: @Composable () -> Unit,
    toolbar: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) { preview() }
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(0.15f).fillMaxWidth()) { toolbar() }
            Box(modifier = Modifier.weight(0.85f).fillMaxWidth()) { timeline() }
        }
    }
}
