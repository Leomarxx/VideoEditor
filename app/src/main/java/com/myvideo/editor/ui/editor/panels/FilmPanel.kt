package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.ui.editor.EditorViewModel

@Composable
fun FilmPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    var grain by remember { mutableFloatStateOf(30f) }
    var scratches by remember { mutableFloatStateOf(0f) }
    var dust by remember { mutableFloatStateOf(0f) }
    var flicker by remember { mutableFloatStateOf(0f) }
    var fade by remember { mutableFloatStateOf(0f) }
    var halation by remember { mutableFloatStateOf(0f) }
    var filmStock by remember { mutableStateOf("Kodak") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("胶片预设", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Kodak", "Fuji", "Agfa", "Ilford").forEach { f ->
                OptionChip(f, filmStock == f) { filmStock = f }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("颗粒: ${"%.0f".format(grain)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("颗粒", 0, grain.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("划痕: ${"%.0f".format(scratches)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("划痕", 0, scratches.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("灰尘: ${"%.0f".format(dust)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("灰尘", 0, dust.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("闪烁: ${"%.0f".format(flicker)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("闪烁", 0, flicker.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("褪色: ${"%.0f".format(fade)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("褪色", 0, fade.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("光晕: ${"%.0f".format(halation)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("光晕", 0, halation.toInt(), 100)

        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) {
                vm.showToast("胶片效果: $filmStock 颗粒${"%.0f".format(grain)}%")
            } else { vm.showToast("请先选择片段") }
            onClose()
        }
    }
}
