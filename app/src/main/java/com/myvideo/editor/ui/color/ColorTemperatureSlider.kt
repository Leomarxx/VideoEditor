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
fun ColorTemperatureSlider(
    value: Float = 0.5f,
    onValueChanged: (Float) -> Unit = {}
) {
    var localValue by remember { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("色温", fontSize = 11.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp).pointerInput(Unit) {
            detectDragGestures { change, _ ->
                localValue = (change.position.x / size.width).coerceIn(0f, 1f)
                onValueChanged(localValue)
            }
        }) {
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF4488FF), Color(0xFFCCCCCC), Color(0xFFFF8844))),
                cornerRadius = CornerRadius(8f, 8f), size = Size(size.width, size.height))
            drawCircle(Color.White, 10f, Offset(localValue * size.width, size.height / 2f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("冷", fontSize = 9.sp, color = Color(0xFF4488FF))
            Text("暖", fontSize = 9.sp, color = Color(0xFFFF8844))
        }
    }
}
