package com.myvideo.editor.ui.color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColorGradingPanel(
    onColorChanged: (Map<String, Any>) -> Unit = {}
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var lightness by remember { mutableStateOf(0.5f) }
    var temperature by remember { mutableStateOf(0.5f) }
    var tint by remember { mutableStateOf(0.5f) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ColorWheelPicker(hue = hue, saturation = saturation) { h, s ->
            hue = h; saturation = s
            onColorChanged(mapOf("hue" to h, "saturation" to s))
        }
        Spacer(modifier = Modifier.height(12.dp))
        LuminanceSlider(value = lightness) { lightness = it }
        Spacer(modifier = Modifier.height(12.dp))
        ColorTemperatureSlider(value = temperature) { temperature = it }
        Spacer(modifier = Modifier.height(12.dp))
        TintSlider(value = tint) { tint = it }
        Spacer(modifier = Modifier.height(12.dp))
        HSLColorPicker(hue, saturation, lightness) { h, s, l ->
            hue = h; saturation = s; lightness = l
        }
        Spacer(modifier = Modifier.height(12.dp))
        ThreeWayColorWheel()
        Spacer(modifier = Modifier.height(12.dp))
        HSLSecondaryPanel()
        Spacer(modifier = Modifier.height(12.dp))
        ColorGradingNodeGraph()
        Spacer(modifier = Modifier.height(12.dp))
        ColorGradingPresetsPanel { name ->
            onColorChanged(mapOf("preset" to name))
        }
    }
}
