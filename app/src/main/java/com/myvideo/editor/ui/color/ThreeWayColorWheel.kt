package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThreeWayColorWheel(
    shadowsHue: Float = 0f,
    midtonesHue: Float = 0f,
    highlightsHue: Float = 0f,
    onChanged: (Float, Float, Float) -> Unit = { _, _, _ -> }
) {
    var sh by remember { mutableStateOf(shadowsHue) }
    var mh by remember { mutableStateOf(midtonesHue) }
    var hh by remember { mutableStateOf(highlightsHue) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("三路调色", fontSize = 11.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            listOf("阴影" to sh, "中间调" to mh, "高光" to hh).forEach { (label, hue) ->
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        for (angle in 0 until 360) {
                            val rad = Math.toRadians(angle.toDouble())
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawLine(Color.hsv(angle.toFloat(), 1f, 1f),
                                Offset(cx + 30f * kotlin.math.cos(rad).toFloat(), cy + 30f * kotlin.math.sin(rad).toFloat()),
                                Offset(cx + 40f * kotlin.math.cos(rad).toFloat(), cy + 40f * kotlin.math.sin(rad).toFloat()),
                                strokeWidth = 6f)
                        }
                    }
                    Text(label, fontSize = 9.sp, color = Color(0xFF666666))
                }
            }
        }
    }
}
