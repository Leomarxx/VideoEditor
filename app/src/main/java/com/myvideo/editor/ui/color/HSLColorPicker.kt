package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HSLColorPicker(
    hue: Float = 0f,
    saturation: Float = 1f,
    lightness: Float = 0.5f,
    onChanged: (Float, Float, Float) -> Unit = { _, _, _ -> }
) {
    var h by remember { mutableStateOf(hue) }
    var s by remember { mutableStateOf(saturation) }
    var l by remember { mutableStateOf(lightness) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("HSL 精细调色", fontSize = 11.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(8.dp))
        HSLSlider("色相", 0f, 360f, h, Color.hsv(h, 1f, 1f)) { h = it; onChanged(h, s, l) }
        HSLSlider("饱和度", 0f, 1f, s, Color.hsv(h, s, 1f)) { s = it; onChanged(h, s, l) }
        HSLSlider("明度", 0f, 1f, l, Color.hsv(h, s, l)) { l = it; onChanged(h, s, l) }
    }
}

@Composable
private fun HSLSlider(label: String, min: Float, max: Float, value: Float, color: Color, onChanged: (Float) -> Unit) {
    var localValue by remember { mutableStateOf(value) }
    Text("$label: ${"%.1f".format(localValue)}", fontSize = 10.sp, color = Color(0xFFCCCCCC))
    Spacer(modifier = Modifier.height(4.dp))
    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp).pointerInput(Unit) {
        detectDragGestures { change, _ ->
            localValue = (change.position.x / size.width).coerceIn(0f, 1f) * (max - min) + min
            onChanged(localValue)
        }
    }) {
        drawRoundRect(Brush.horizontalGradient(listOf(Color.Black, color)),
            cornerRadius = CornerRadius(6f, 6f), size = Size(size.width, size.height))
        drawCircle(Color.White, 8f, Offset((localValue - min) / (max - min) * size.width, size.height / 2f))
    }
    Spacer(modifier = Modifier.height(8.dp))
}
