package com.myvideo.editor.ui.curves

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CurveGrid(modifier: Modifier = Modifier, divisions: Int = 4) {
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        for (i in 1 until divisions) {
            drawLine(Color(0xFF2A2A2A), Offset(size.width * i / divisions, 0f), Offset(size.width * i / divisions, size.height), strokeWidth = 1f)
            drawLine(Color(0xFF2A2A2A), Offset(0f, size.height * i / divisions), Offset(size.width, size.height * i / divisions), strokeWidth = 1f)
        }
        drawLine(Color(0xFF333333), Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = 1f)
    }
}
