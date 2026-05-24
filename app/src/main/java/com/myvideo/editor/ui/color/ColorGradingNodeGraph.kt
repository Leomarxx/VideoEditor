package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorGradingNodeGraph(
    nodes: List<Offset> = emptyList(),
    onNodeChanged: (List<Offset>) -> Unit = {}
) {
    var localNodes by remember {
        mutableStateOf(nodes.ifEmpty {
            listOf(Offset(0.2f, 0.3f), Offset(0.5f, 0.5f), Offset(0.8f, 0.7f))
        })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("节点图", fontSize = 11.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    val y = (change.position.y / size.height).coerceIn(0f, 1f)
                    val nearest = localNodes.minByOrNull {
                        kotlin.math.sqrt((it.x - x) * (it.x - x) + (it.y - y) * (it.y - y))
                    }
                    if (nearest != null) {
                        val idx = localNodes.indexOf(nearest)
                        val newList = localNodes.toMutableList()
                        newList[idx] = Offset(x, y)
                        localNodes = newList
                        onNodeChanged(localNodes)
                    }
                }
            }
        ) {
            // 连线
            val path = Path()
            localNodes.sortedBy { it.x }.forEachIndexed { i, node ->
                val px = node.x * size.width
                val py = node.y * size.height
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, Color(0xFF4A90D9), style = Stroke(width = 2f))
            // 节点
            localNodes.forEach { node ->
                drawCircle(Color.White, 8f, Offset(node.x * size.width, node.y * size.height))
                drawCircle(Color(0xFF4A90D9), 6f, Offset(node.x * size.width, node.y * size.height))
            }
        }
    }
}
