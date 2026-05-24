package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ColorWheelPicker(
    hue: Float = 0f,
    saturation: Float = 1f,
    onColorChanged: (Float, Float) -> Unit = { _, _ -> }
) {
    var h by remember { mutableStateOf(hue) }
    var s by remember { mutableStateOf(saturation) }
    Canvas(modifier = Modifier.size(200.dp).pointerInput(Unit) {
        detectDragGestures { change, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val dx = change.position.x - cx
            val dy = change.position.y - cy
            val r = minOf(cx, cy)
            val dist = minOf(kotlin.math.sqrt(dx * dx + dy * dy).toFloat(), r)
            h = (kotlin.math.atan2(dy, dx) * 180f / Math.PI.toFloat() + 360f) % 360f
            s = dist / r
            onColorChanged(h, s)
        }
    }) {
        for (angle in 0 until 360) {
            val rad = Math.toRadians(angle.toDouble())
            drawLine(Color.hsv(angle.toFloat(), 1f, 1f),
                Offset(center.x + 80f * kotlin.math.cos(rad).toFloat(), center.y + 80f * kotlin.math.sin(rad).toFloat()),
                Offset(center.x + 100f * kotlin.math.cos(rad).toFloat(), center.y + 100f * kotlin.math.sin(rad).toFloat()),
                strokeWidth = 8f)
        }
        val px = center.x + 90f * s * kotlin.math.cos(Math.toRadians(h.toDouble())).toFloat()
        val py = center.y + 90f * s * kotlin.math.sin(Math.toRadians(h.toDouble())).toFloat()
        drawCircle(Color.White, 10f, Offset(px, py))
    }
}
