package com.myvideo.editor.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TabletLayout(
    preview: @Composable () -> Unit,
    timeline: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
    panels: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.15f).fillMaxHeight()) { toolbar() }
        Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) { preview() }
            Box(modifier = Modifier.weight(0.4f).fillMaxWidth()) { timeline() }
        }
        Box(modifier = Modifier.weight(0.3f).fillMaxHeight()) { panels() }
    }
}
