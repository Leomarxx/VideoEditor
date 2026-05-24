package com.myvideo.editor.ui.curves

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CurvePanel(
    points: List<Offset> = listOf(Offset(0f, 0f), Offset(1f, 1f)),
    onPointsChanged: (List<Offset>) -> Unit = {},
    curveName: String = "RGB"
) {
    var localPoints by remember { mutableStateOf(points) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$curveName 曲线", fontSize = 11.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    val newPoints = localPoints.toMutableList()
                    val idx = newPoints.indexOfFirst { kotlin.math.abs(it.x - x) < 0.1f }
                    if (idx >= 0) {
                        newPoints[idx] = Offset(x, y)
                        localPoints = newPoints
                        onPointsChanged(localPoints)
                    }
                }
            }
        ) {
            for (i in 1..3) {
                drawLine(Color(0xFF2A2A2A), Offset(size.width * i / 4, 0f), Offset(size.width * i / 4, size.height))
                drawLine(Color(0xFF2A2A2A), Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4))
            }
            val path = Path()
            localPoints.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.x * size.width, (1f - p.y) * size.height)
                else path.lineTo(p.x * size.width, (1f - p.y) * size.height)
            }
            drawPath(path, Color(0xFF4A90D9), style = Stroke(width = 3f))
            localPoints.forEach { p ->
                drawCircle(Color.White, 6f, Offset(p.x * size.width, (1f - p.y) * size.height))
            }
        }
    }
}
