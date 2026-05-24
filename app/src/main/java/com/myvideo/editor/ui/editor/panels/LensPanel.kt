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
fun LensPanel(vm: EditorViewModel = EditorViewModel(), onClose: () -> Unit = {}) {
    var vignette by remember { mutableFloatStateOf(0f) }
    var flare by remember { mutableFloatStateOf(0f) }
    var flareX by remember { mutableFloatStateOf(50f) }
    var flareY by remember { mutableFloatStateOf(30f) }
    var flareSize by remember { mutableFloatStateOf(40f) }
    var distortion by remember { mutableFloatStateOf(0f) }
    var chromaticAberration by remember { mutableFloatStateOf(0f) }
    var bloom by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("暗角", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("强度: ${"%.0f".format(vignette)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("暗角", 0, vignette.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("光晕", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("强度: ${"%.0f".format(flare)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("光晕强度", 0, flare.toInt(), 100)
        Spacer(modifier = Modifier.height(6.dp))
        Text("X位置: ${"%.0f".format(flareX)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("光晕X", 0, flareX.toInt(), 100)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Y位置: ${"%.0f".format(flareY)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("光晕Y", 0, flareY.toInt(), 100)
        Spacer(modifier = Modifier.height(6.dp))
        Text("大小: ${"%.0f".format(flareSize)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("光晕大小", 0, flareSize.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("镜头畸变", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("强度: ${"%.0f".format(distortion)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("畸变", -50, distortion.toInt() + 50, 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("色散", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("强度: ${"%.0f".format(chromaticAberration)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("色散", 0, chromaticAberration.toInt(), 100)
        Spacer(modifier = Modifier.height(10.dp))

        Text("泛光", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("强度: ${"%.0f".format(bloom)}%", fontSize = 10.sp, color = CG.T2)
        CgSlider("泛光", 0, bloom.toInt(), 100)

        Spacer(modifier = Modifier.height(16.dp))
        ApplyButton("应用") {
            val clip = vm.selectedClip()
            if (clip != null) {
                vm.showToast("镜头效果: 暗角${"%.0f".format(vignette)}% 光晕${"%.0f".format(flare)}%")
            } else { vm.showToast("请先选择片段") }
            onClose()
        }
    }
}
