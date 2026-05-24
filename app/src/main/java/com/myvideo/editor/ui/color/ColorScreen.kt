package com.myvideo.editor.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object CC {
    val Bg=Color(0xFF0A0A0A); val Surf=Color(0xFF111111); val Card=Color(0xFF181818)
    val Acc=Color(0xFF4A90D9); val Acc2=Color(0xFF6EC850); val Gold=Color(0xFFE8A820)
    val Green=Color(0xFF6EC850); val Red=Color(0xFFE84848)
    val T1=Color(0xFFF0ECE4); val T2=Color(0xFFB0ACA4); val T3=Color(0xFF6A6660)
    val Line=Color(0xFF222222)
}

@Composable
fun ColorScreen(onBack: () -> Unit = {}) {
    var tab by remember { mutableStateOf("wheels") }
    var playing by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(CC.Bg)) {
        // 顶栏
        Row(modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(CC.Card)
                .clickable { onBack() }, contentAlignment = Alignment.Center) { Text("‹", fontSize = 20.sp, color = CC.T2) }
            Text("调色", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CC.T1)
            Text("应用", fontSize = 12.sp, color = CC.Acc, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CC.Acc.copy(0.12f))
                    .clickable { onBack() }.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        // 预览
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color(0xFF050505)), contentAlignment = Alignment.Center) {
            Text("1920×1080 · 30fps", fontSize = 9.sp, color = CC.T3, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(4.dp))
        // 标签
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("wheels" to "色轮","curves" to "曲线","hsl" to "色相","levels" to "色阶",
                "balance" to "平衡","lut" to "滤镜","sharpen" to "锐化","audio" to "音频").forEach { (k,v) ->
                val on=tab==k
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if(on)CC.Acc.copy(0.15f)else Color.Transparent)
                    .clickable{tab=k}.padding(horizontal=10.dp,vertical=5.dp)) {
                    Text(v, fontSize=11.sp, color=if(on)CC.Acc else CC.T3, fontWeight=if(on)FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        // 内容
        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column { when(tab) {
                "wheels"->WheelsPage(); "curves"->CurvesPage(); "hsl"->HslPage()
                "levels"->LevelsPage(); "balance"->BalancePage(); "lut"->LutPage()
                "sharpen"->SharpenPage(); "audio"->AudioPage()
            }; Spacer(modifier = Modifier.height(8.dp)) }
        }
        // 播放栏
        Row(modifier = Modifier.fillMaxWidth().height(40.dp).background(CC.Surf)
            .border(1.dp,CC.Line).padding(horizontal=16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("00:01.10",fontSize=10.sp,color=CC.T2,fontFamily=FontFamily.Monospace)
            Spacer(modifier = Modifier.width(8.dp))
            listOf("⏮","⏪",if(playing)"⏸"else"▶","⏩","⏭").forEach { i ->
                val main=i=="▶"||i=="⏸"
                Box(modifier = Modifier.size(if(main)36.dp else 30.dp).clip(RoundedCornerShape(8.dp))
                    .background(if(main)CC.Acc.copy(0.2f)else Color.Transparent)
                    .clickable{if(main)playing=!playing}, contentAlignment=Alignment.Center) {
                    Text(i,fontSize=if(main)16.sp else 11.sp,color=CC.T1)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("00:01.70",fontSize=10.sp,color=CC.T3,fontFamily=FontFamily.Monospace)
        }
    }
}

// ===== 色轮页 =====
@Composable
private fun WheelsPage() {
    Sec("色轮") {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            listOf("阴影","中间调","高光").forEach { label ->
                ColorWheel(label)
            }
        }
    }
    Sec("亮度") { CcSlider("亮度", "+0.2", 60f) }
    Sep()
    Sec("基础校正") {
        CcSlider("曝光", "+4", 52f)
        CcSlider("对比度", "+16", 58f)
        CcSlider("高光", "-16", 42f)
        CcSlider("阴影", "+20", 60f)
    }
    Sep()
    Sec("白平衡") {
        CcSlider("色温", "-12", 44f, listOf(Color(0xFF4488CC), Color(0xFFCC8844)))
        CcSlider("色调", "+8", 54f, listOf(Color(0xFF44CC44), Color(0xFFCC44CC)))
    }
    Sep()
    Sec("饱和度") {
        CcSlider("饱和度", "-24", 38f, listOf(Color(0xFF666666), CC.Acc2))
        CcSlider("自然饱和", "+24", 62f, listOf(Color(0xFF666666), CC.Acc2))
    }
}

@Composable
private fun ColorWheel(label: String) {
    var dotPos by remember { mutableStateOf(Offset(0f, 0f)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val r = size.width / 2f - 8.dp.toPx()
                    val dist = sqrt(dx * dx + dy * dy)
                    dotPos = if (dist <= r) Offset(dx, dy) else Offset(dx * r / dist, dy * r / dist)
                }
            }) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.width / 2f - 8.dp.toPx()
                for (angle in 0 until 360 step 5) {
                    val rad = Math.toRadians(angle.toDouble())
                    drawLine(Color.hsl(angle.toFloat(), 0.6f, 0.5f),
                        c, Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat()), strokeWidth = 6.dp.toPx())
                }
                drawCircle(Color(0xFF1A1A1A), r - 6.dp.toPx(), c)
                drawCircle(Color.White.copy(0.7f), 4.dp.toPx(), Offset(c.x + dotPos.x, c.y + dotPos.y))
            }
        }
        Text(label, fontSize = 9.sp, color = CC.T3, modifier = Modifier.padding(top = 4.dp))
    }
}

// ===== 曲线页 =====
@Composable
private fun CurvesPage() {
    var ch by remember { mutableStateOf("rgb") }
    Sec("曲线 · 拖拽控制点") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("rgb" to "总曲线" to Color.White, "r" to "红" to Color(0xFFE04848),
                "g" to "绿" to Color(0xFF6EC850), "b" to "蓝" to Color(0xFF6AAFE6)).forEach { item ->
                // flatMap below won't work, fix the syntax
            }
        }
        // 修复：用正确写法
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data class Ch(val id: String, val label: String, val color: Color)
            listOf(Ch("rgb","总曲线",Color.White), Ch("r","红",Color(0xFFE04848)),
                Ch("g","绿",Color(0xFF6EC850)), Ch("b","蓝",Color(0xFF6AAFE6))).forEach { c ->
                val on = ch == c.id
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (on) CC.Acc.copy(0.15f) else CC.Card)
                    .clickable { ch = c.id }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(c.label, fontSize = 10.sp, color = if (on) c.color else CC.T3)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CurveCanvas()
    }
    Sec("预设") {
        OptRow(listOf("无","柔和","中等","强烈","胶片","褪色"), "中等") {}
    }
}

@Composable
private fun CurveCanvas() {
    val pts = remember { mutableStateListOf(Offset(0f, 1f), Offset(0.33f, 0.6f), Offset(0.66f, 0.4f), Offset(1f, 0f)) }
    Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)).background(CC.Card)) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                change.consume()
                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                val ny = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                val closest = pts.indices.minByOrNull { val p = pts[it]; sqrt((p.x - nx).pow(2) + (p.y - ny).pow(2)) } ?: return@detectDragGestures
                pts[closest] = Offset(nx, ny)
            }
        }) {
            val w = size.width; val h = size.height
            // 网格
            for (i in 1..3) { val x = w * i / 4; drawLine(CC.Line2, Offset(x, 0f), Offset(x, h), 1f)
                val y = h * i / 4; drawLine(CC.Line2, Offset(0f, y), Offset(w, y), 1f) }
            // 曲线
            val path = Path()
            pts.forEachIndexed { i, p ->
                val sx = p.x * w; val sy = (1f - p.y) * h
                if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
            }
            drawPath(path, CC.Acc, style = Stroke(width = 2.dp.toPx()))
            // 控制点
            pts.forEach { p -> drawCircle(Color.White, 5.dp.toPx(), Offset(p.x * w, (1f - p.y) * h)) }
        }
    }
}
private val CC.Line2 get() = Color(0xFF2A2A2A)

// ===== 色相页 =====
@Composable
private fun HslPage() {
    Sec("色相 / 饱和度 / 明度") {
        listOf("红" to Color(0xFFE83030), "黄" to Color(0xFFE8C830), "绿" to Color(0xFF30E830),
            "青" to Color(0xFF30C8E8), "蓝" to Color(0xFF3030E8), "洋红" to Color(0xFFE830E8)).forEach { (name, clr) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(clr))
                Text(name, fontSize = 10.sp, color = CC.T2, modifier = Modifier.width(28.dp))
                CcSlider("色相", "0", 50f, listOf(clr, clr))
                CcSlider("饱和", "0", 50f, listOf(clr, clr))
                CcSlider("明度", "0", 50f, listOf(clr, clr))
            }
        }
    }
}

// ===== 色阶页 =====
@Composable
private fun LevelsPage() {
    Sec("色阶") {
        // 直方图
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp)).background(CC.Card)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 64) {
                    val h = (sin(i * 0.15) * 0.3 + 0.4 + sin(i * 0.3) * 0.2).coerceIn(0.05, 1.0)
                    drawRect(Color(0xFF6A6660), Offset(i * size.width / 64, size.height * (1 - h.toFloat())),
                        Size(size.width / 64 - 1, size.height * h.toFloat()))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF444444), Color.White))))
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0","64","128","192","255").forEach { Text(it, fontSize = 8.sp, color = CC.T3, fontFamily = FontFamily.Monospace) }
        }
        CcSlider("输入黑", "20", 8f)
        CcSlider("灰点", "1.15", 45f)
        CcSlider("输入白", "235", 92f)
        CcSlider("输出黑", "12", 5f)
        CcSlider("输出白", "245", 95f)
    }
    Sec("通道") { OptRow(listOf("总","红","绿","蓝"), "总") {} }
}

// ===== 色彩平衡页 =====
@Composable
private fun BalancePage() {
    Sec("色彩平衡 · 拖拽圆内调色") {
        listOf("阴影","中间","高光").forEach { zone ->
            Text(zone, fontSize = 10.sp, color = CC.T2, modifier = Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("青↔红","洋↔绿","黄↔蓝").forEach { BalanceWheel(it) }
            }
        }
    }
    Sep()
    Sec("示例颜色") {
        Row { listOf(Color.Black, Color(0xFF1A0A2E), Color(0xFF0A1A2E)).forEach { c ->
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c)
                .border(1.dp, CC.Line, CircleShape).padding(2.dp)) } }
        Spacer(modifier = Modifier.height(4.dp))
        Row { listOf(Color.White, Color(0xFFF0C040), Color(0xFF40A0F0)).forEach { c ->
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c)
                .border(1.dp, CC.Line, CircleShape).padding(2.dp)) } }
    }
}

@Composable
private fun BalanceWheel(label: String) {
    var dotPos by remember { mutableStateOf(Offset(0f, 0f)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val c = Offset(size.width/2f, size.height/2f)
                    val dx = change.position.x - c.x; val dy = change.position.y - c.y
                    val r = size.width/2f - 6.dp.toPx()
                    val dist = sqrt(dx*dx+dy*dy)
                    dotPos = if(dist<=r) Offset(dx,dy) else Offset(dx*r/dist, dy*r/dist)
                }
            }) {
                val c = Offset(size.width/2f, size.height/2f)
                val r = size.width/2f - 6.dp.toPx()
                drawCircle(Color(0xFF2A2A2A), r, c)
                for(a in 0 until 360 step 10) {
                    val rad = Math.toRadians(a.toDouble())
                    drawLine(Color.hsl(a.toFloat(),0.5f,0.4f), c,
                        Offset(c.x+r*cos(rad).toFloat(), c.y+r*sin(rad).toFloat()), strokeWidth=4.dp.toPx())
                }
                drawCircle(Color(0xFF1A1A1A), r-5.dp.toPx(), c)
                drawCircle(Color.White.copy(0.8f), 3.dp.toPx(), Offset(c.x+dotPos.x, c.y+dotPos.y))
            }
        }
        Text(label, fontSize = 8.sp, color = CC.T3)
    }
}

// ===== 滤镜页 =====
@Composable
private fun LutPage() {
    Sec("滤镜预设") {
        val luts = listOf("原图" to Color(0xFF888888), "暖阳" to Color(0xFFE8A830), "冷调" to Color(0xFF4A90D9),
            "胶片" to Color(0xFFA08060), "黑白" to Color(0xFF606060), "鲜艳" to Color(0xFFE84848),
            "复古" to Color(0xFFC0A060), "清透" to Color(0xFF80C8E0), "夜色" to Color(0xFF203040))
        val sel = remember { mutableStateOf("原图") }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            luts.forEach { (name, clr) ->
                val on = sel.value == name
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { sel.value = name }) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(clr)
                        .then(if(on) Modifier.border(2.dp, CC.Acc, RoundedCornerShape(6.dp)) else Modifier))
                    Text(name, fontSize = 8.sp, color = if(on) CC.Acc else CC.T3, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
    CcSlider("强度", "75", 75f)
}

// ===== 锐化页 =====
@Composable
private fun SharpenPage() {
    Sec("锐化") { CcSlider("锐化量", "30", 30f); CcSlider("半径", "1.0", 33f); CcSlider("降噪", "20", 20f) }
    Sep()
    Sec("暗角") { CcSlider("强度", "-28", 28f); CcSlider("羽化", "65", 65f) }
    Sep()
    Sec("颗粒") { CcSlider("强度", "25", 25f); CcSlider("大小", "2.0", 40f) }
}

// ===== 音频页 =====
@Composable
private fun AudioPage() {
    Sec("波形预览") {
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(6.dp)).background(CC.Card)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 200) {
                    val h = (sin(i * 0.08) * 0.3 + 0.4 + sin(i * 0.2) * 0.2).coerceIn(0.05, 1.0)
                    val x = i * size.width / 200
                    drawRect(if(i < 70) CC.Acc else Color(0xFF444444), Offset(x, size.height*(1-h.toFloat())/2),
                        Size(size.width/200-1, size.height*h.toFloat()))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:12", fontSize = 8.sp, color = CC.T3, fontFamily = FontFamily.Monospace)
            Text("01:24", fontSize = 8.sp, color = CC.T3, fontFamily = FontFamily.Monospace)
        }
    }
    Sec("电平表") {
        Row(modifier = Modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            val levels = listOf(28,26,25,24,22,20,18,16,14,12,10,8,6,4,3,2)
            levels.forEachIndexed { i, v -> val ratio = v / 28f
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(1.dp))
                    .background(if(ratio > 0.85f) CC.Red else if(ratio > 0.6f) CC.Gold else CC.Green.copy(0.6f)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("-48","-24","-12","-6","0","+3").forEach { Text(it, fontSize = 7.sp, color = CC.T3, fontFamily = FontFamily.Monospace) }
        }
    }
    Sep()
    Sec("音量控制") {
        CcSlider("主音量", "-6dB", 70f, listOf(CC.Green, CC.Gold, CC.Red))
        CcSlider("淡入", "0.5秒", 10f)
        CcSlider("淡出", "1.2秒", 24f)
    }
    Sep()
    Sec("均衡器 · 上下拖拽") {
        EqBars()
    }
    Sep()
    Sec("音效") {
        OptRow(listOf("无","混响","回声","降噪","压缩器"), "混响") {}
        Spacer(modifier = Modifier.height(6.dp))
        CcSlider("混响量", "35", 35f)
        CcSlider("房间", "60", 60f)
    }
    Sep()
    Sec("素材库") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("全部","音乐","音效","环境").forEach { o -> val on = o == "全部"
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if(on) CC.Acc.copy(0.15f) else CC.Card)
                    .clickable{}.padding(horizontal=10.dp,vertical=5.dp)) {
                    Text(o, fontSize=10.sp, color=if(on)CC.Acc else CC.T3)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        listOf(Triple("轻柔钢琴","02:15 · C大调",true), Triple("Lo-Fi 节拍","03:42 · 85BPM",true),
            Triple("史诗配乐","04:20 · 140BPM",false), Triple("雨声白噪","10:00 · 环境",true)).forEach { (name, desc, free) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(8.dp))
                .background(CC.Card).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                    .background(if(free) CC.Green.copy(0.1f) else CC.Gold.copy(0.1f)),
                    contentAlignment = Alignment.Center) { Text("▶", fontSize = 10.sp, color = if(free) CC.Green else CC.Gold) }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 11.sp, color = CC.T1, fontWeight = FontWeight.Medium)
                    Text(desc, fontSize = 8.sp, color = CC.T3)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if(free) CC.Green.copy(0.12f) else CC.Gold.copy(0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(if(free)"免费" else "PRO", fontSize = 8.sp, color = if(free) CC.Green else CC.Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EqBars() {
    val freqs = listOf("31","62","125","250","500","1K","2K","4K","8K","16K")
    val heights = remember { mutableStateListOf(50f,55f,65f,60f,50f,70f,75f,65f,55f,45f) }
    Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        freqs.forEachIndexed { i, f ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.width(16.dp).height(100.dp).pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        heights[i] = (100f - change.position.y / size.height * 100f).coerceIn(10f, 95f)
                    }
                }) {
                    val h = heights[i] / 100f
                    drawRoundRect(CC.Card, Offset(0f, 0f), Size(size.width, size.height), CornerRadius(4.dp.toPx()))
                    drawRoundRect(CC.Acc.copy(0.5f), Offset(0f, size.height * (1 - h)),
                        Size(size.width, size.height * h), CornerRadius(4.dp.toPx()))
                    drawCircle(Color.White, 4.dp.toPx(), Offset(size.width / 2, size.height * (1 - h)))
                }
                Text(f, fontSize = 7.sp, color = CC.T3, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
