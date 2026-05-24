package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object CG {
    val Card = Color(0xFF2C2C2C); val Line = Color(0xFF3A3A3A)
    val T2 = Color(0xFF999999); val T3 = Color(0xFF666666)
    val T4 = Color(0xFF4A4A4A); val Acc = Color(0xFF4A90D9)
    val AccL = Color(0xFF6AAFE6); val AccS = Color(0x1F4A90D9)
    val Red = Color(0xFFE85050); val Green = Color(0xFF7EC850)
}

@Composable
fun ColorGradingPanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onClose: () -> Unit = {}) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("色轮", "曲线", "HSL", "色阶", "色彩平衡", "LUT")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(30.dp).border(1.dp, CG.Line)) {
            tabs.forEachIndexed { i, t ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Text(t, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (tab == i) CG.Acc else CG.T3)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp, 16.dp)) {
            when (tab) { 0 -> WheelsTab(); 1 -> CurvesTab(); 2 -> HSLTab(); 3 -> LevelsTab(); 4 -> BalanceTab(); 5 -> LUTTab() }
        }
    }
}

@Composable
private fun WheelsTab() {
    Text("色轮", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        listOf("阴影", "中间调", "高光").forEach { l ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, CG.Line, CircleShape).background(CG.Card), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                }
                Text(l, fontSize = 8.sp, color = CG.T3, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    CgSlider("亮度", 0, 50, 100); CgSlider("对比度", 0, 50, 100); CgSlider("饱和度", 0, 50, 100)
    Spacer(modifier = Modifier.height(14.dp)); Text("白平衡", fontSize = 9.sp, color = CG.T4)
    CgSlider("色温", -100, 0, 100); CgSlider("色调", -100, 0, 100)
    Spacer(modifier = Modifier.height(14.dp)); Text("暗角", fontSize = 9.sp, color = CG.T4)
    CgSlider("强度", 0, 0, 100); CgSlider("羽化", 0, 50, 100)
}

@Composable
private fun CurvesTab() {
    Text("RGB 曲线", fontSize = 9.sp, color = CG.T4)
    Spacer(modifier = Modifier.height(6.dp))
    listOf("RGB" to CG.AccL, "R" to CG.Red, "G" to CG.Green, "B" to CG.AccL).forEach { (ch, c) ->
        Text(ch, fontSize = 9.sp, color = CG.T3); Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(CG.Card).border(1.dp, CG.Line)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width; val h = size.height
                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)
                drawPath(Path().apply { moveTo(0f, h); cubicTo(w * 0.3f, h * 0.6f, w * 0.6f, h * 0.3f, w, 0f) }, color = c, style = Stroke(3f))
                listOf(Offset(0f, h), Offset(w * 0.3f, h * 0.6f), Offset(w * 0.6f, h * 0.3f), Offset(w, 0f)).forEach { drawCircle(Color.White, 6f, it) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Text("S曲线预设", fontSize = 9.sp, color = CG.T4); Spacer(modifier = Modifier.height(6.dp))
    var s by remember { mutableStateOf("无") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("无", "柔和", "中等", "强烈", "胶片").forEach { OptionChip(it, s == it) { s = it } }
    }
}

@Composable
private fun HSLTab() {
    Text("HSL 调整", fontSize = 9.sp, color = CG.T4); Spacer(modifier = Modifier.height(6.dp))
    listOf("红", "黄", "绿", "青", "蓝", "洋红").forEach { c ->
        CgSlider(c, -100, 0, 100); CgSlider("饱和", -100, 0, 100); CgSlider("明度", -100, 0, 100)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun LevelsTab() {
    Text("色阶", fontSize = 9.sp, color = CG.T4); Spacer(modifier = Modifier.height(6.dp))
    CgSlider("输入黑", 0, 0, 255); CgSlider("输入灰", 0, 128, 255); CgSlider("输入白", 0, 255, 255)
    CgSlider("输出黑", 0, 0, 255); CgSlider("输出白", 0, 255, 255)
    Spacer(modifier = Modifier.height(14.dp)); Text("通道", fontSize = 9.sp, color = CG.T4)
    var ch by remember { mutableStateOf("RGB") }
    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("RGB", "R", "G", "B").forEach { OptionChip(it, ch == it) { ch = it } }
    }
}

@Composable
private fun BalanceTab() {
    Text("色彩平衡", fontSize = 9.sp, color = CG.T4); Spacer(modifier = Modifier.height(6.dp))
    listOf("阴影", "中间调", "高光").forEach { z ->
        Text(z, fontSize = 9.sp, color = CG.T4, modifier = Modifier.padding(top = 8.dp))
        CgSlider("青-红", -100, 0, 100); CgSlider("洋红-绿", -100, 0, 100); CgSlider("黄-蓝", -100, 0, 100)
    }
}

@Composable
private fun LUTTab() {
    Text("LUT 预设", fontSize = 9.sp, color = CG.T4); Spacer(modifier = Modifier.height(6.dp))
    var sel by remember { mutableStateOf("无") }
    val luts = listOf("无" to Color.Transparent, "电影" to Color(0xFF2A1A0A), "冷色" to Color(0xFF0A1A2E), "暖色" to Color(0xFF2E1A0A), "胶片" to Color(0xFF1A1A1A), "复古" to Color(0xFF2A2015), "清新" to Color(0xFFD0E8D0), "赛博" to Color(0xFF0A0A2E))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        luts.forEach { (n, c) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(50.dp, 36.dp).clip(RoundedCornerShape(6.dp)).background(c)
                    .then(if (sel == n) Modifier.border(2.dp, CG.Acc, RoundedCornerShape(6.dp)) else Modifier)
                    .clickable { sel = n }, contentAlignment = Alignment.Center) {
                    if (c == Color.Transparent) Text("无", fontSize = 8.sp, color = CG.T2)
                }
                Text(n, fontSize = 6.sp, color = Color.White.copy(alpha = .7f))
            }
        }
    }
    Spacer(modifier = Modifier.height(14.dp)); CgSlider("强度", 0, 100, 100)
    Spacer(modifier = Modifier.height(14.dp)); Text("锐化", fontSize = 9.sp, color = CG.T4)
    CgSlider("锐化量", 0, 0, 100); CgSlider("半径", 1, 1, 3)
    Spacer(modifier = Modifier.height(12.dp)); ApplyButton("应用调色")
}

@Composable
internal fun CgSlider(label: String, min: Int, value: Int, max: Int) {
    var v by remember { mutableStateOf(value) }
    val pct = ((v - min).toFloat() / (max - min) * 100).coerceIn(0f, 100f)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, color = CG.T3, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CG.Card)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).clip(RoundedCornerShape(3.dp)).background(CG.Acc))
        }
        Text("$v", fontSize = 8.sp, color = CG.T2, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

@Composable
internal fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) CG.AccS else CG.Card)
        .then(if (selected) Modifier.border(1.5.dp, CG.Acc, RoundedCornerShape(8.dp)) else Modifier)
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 11.sp, color = if (selected) CG.AccL else CG.T2)
    }
}

@Composable
internal fun ApplyButton(label: String, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
        .background(Brush.linearGradient(listOf(CG.Acc, CG.AccL))).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
