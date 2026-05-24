package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun LuminanceSlider(
    value: Float = 0.5f,
    onValueChanged: (Float) -> Unit = {}
) {
    var localValue by remember { mutableStateOf(value) }
    Canvas(modifier = Modifier.fillMaxWidth().height(24.dp).pointerInput(Unit) {
        detectDragGestures { change, _ ->
            localValue = (change.position.x / size.width).coerceIn(0f, 1f)
            onValueChanged(localValue)
        }
    }) {
        drawRoundRect(Brush.horizontalGradient(listOf(Color.Black, Color.White)),
            cornerRadius = CornerRadius(8f, 8f), size = Size(size.width, size.height))
        drawCircle(Color.White, 12f, Offset(localValue * size.width, size.height / 2f))
    }
}
