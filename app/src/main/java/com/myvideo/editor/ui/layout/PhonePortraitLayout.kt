package com.myvideo.editor.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PhonePortraitLayout(
    preview: @Composable () -> Unit,
    timeline: @Composable () -> Unit,
    toolbar: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.4f).fillMaxWidth()) { preview() }
        Box(modifier = Modifier.weight(0.1f).fillMaxWidth()) { toolbar() }
        Box(modifier = Modifier.weight(0.5f).fillMaxWidth()) { timeline() }
    }
}
