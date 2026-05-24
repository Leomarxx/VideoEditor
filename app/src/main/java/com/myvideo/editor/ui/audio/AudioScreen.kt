package com.myvideo.editor.ui.audio

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
import kotlin.math.*

private object AC {
    val Bg=Color(0xFF0A0A0A); val Surf=Color(0xFF111111); val Card=Color(0xFF181818)
    val Acc=Color(0xFF4A90D9); val Acc2=Color(0xFF6EC850); val Gold=Color(0xFFE8A820)
    val Green=Color(0xFF6EC850); val Red=Color(0xFFE84848)
    val T1=Color(0xFFF0ECE4); val T2=Color(0xFFB0ACA4); val T3=Color(0xFF6A6660)
    val Line=Color(0xFF222222)
}

@Composable
fun AudioScreen(onBack: () -> Unit = {}) {
    var tab by remember { mutableStateOf("mix") }
    var playing by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(AC.Bg)) {
        Row(modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AC.Card)
                .clickable { onBack() }, contentAlignment = Alignment.Center) { Text("‹", fontSize = 20.sp, color = AC.T2) }
            Text("音频编辑", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AC.T1)
            Text("应用", fontSize = 12.sp, color = AC.Acc, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AC.Acc.copy(0.12f))
                    .clickable { onBack() }.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        // 波形
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(8.dp)).background(AC.Card)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 250) {
                    val h = (sin(i*0.06)*0.35+0.4+sin(i*0.18)*0.2).coerceIn(0.05,1.0)
                    drawRect(if(i<88)AC.Acc else Color(0xFF444444), Offset(i*size.width/250, size.height*(1-h.toFloat())/2),
                        Size(size.width/250-1, size.height*h.toFloat()))
                }
            }
        }
        // 标签
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("mix" to "混音","eq" to "均衡","fx" to "音效","lib" to "素材库").forEach { (k,v) ->
                val on=tab==k
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(on)AC.Acc.copy(0.15f)else Color.Transparent)
                    .clickable{tab=k}.padding(horizontal=14.dp,vertical=6.dp)) {
                    Text(v, fontSize=11.sp, color=if(on)AC.Acc else AC.T3, fontWeight=if(on)FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column { when(tab) { "mix"->MixPage(); "eq"->EqPage(); "fx"->FxPage(); "lib"->LibPage() }; Spacer(Modifier.height(8.dp)) }
        }
        // 播放栏
        Row(modifier = Modifier.fillMaxWidth().height(40.dp).background(AC.Surf).border(1.dp,AC.Line)
            .padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) {
            Text("00:01.10",fontSize=10.sp,color=AC.T2,fontFamily=FontFamily.Monospace); Spacer(Modifier.width(8.dp))
            listOf("⏮","⏪",if(playing)"⏸"else"▶","⏩","⏭").forEach { i -> val main=i=="▶"||i=="⏸"
                Box(modifier=Modifier.size(if(main)36.dp else 30.dp).clip(RoundedCornerShape(8.dp))
                    .background(if(main)AC.Acc.copy(0.2f)else Color.Transparent).clickable{if(main)playing=!playing},
                    contentAlignment=Alignment.Center) { Text(i,fontSize=if(main)16.sp else 11.sp,color=AC.T1) }
            }; Spacer(Modifier.width(8.dp))
            Text("00:01.70",fontSize=10.sp,color=AC.T3,fontFamily=FontFamily.Monospace)
        }
    }
}

@Composable private fun MixPage() {
    As("电平") { Row(Modifier.fillMaxWidth().height(14.dp),horizontalArrangement=Arrangement.spacedBy(1.dp)) {
        (0..15).forEach { i -> Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(1.dp))
            .background(if(i<4)AC.Red else if(i<8)AC.Gold else AC.Green.copy(0.6f))) }
    }}
    Asp("音量") { Asl("主音量","-6dB",70f,listOf(AC.Green,AC.Gold,AC.Red)); Asl("淡入","0.5秒",10f); Asl("淡出","1.2秒",24f) }
    Asp("声道") { Asl("左右","C",50f) }
}

@Composable private fun EqPage() {
    As("10段均衡器") {
        val h = remember { mutableStateListOf(50f,55f,65f,60f,50f,70f,75f,65f,55f,45f) }
        val f = listOf("31","62","125","250","500","1K","2K","4K","8K","16K")
        Row(Modifier.fillMaxWidth().height(120.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.Bottom) {
            f.forEachIndexed { i,l -> Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.weight(1f)) {
                Canvas(Modifier.width(16.dp).height(100.dp).pointerInput(Unit){detectDragGestures{c,_->c.consume();h[i]=(100f-c.position.y/size.height*100f).coerceIn(10f,95f)}}) {
                    val hh=h[i]/100f; drawRoundRect(AC.Card,Offset(0f,0f),Size(size.width,size.height),CornerRadius(4.dp.toPx()))
                    drawRoundRect(AC.Acc.copy(0.5f),Offset(0f,size.height*(1-hh)),Size(size.width,size.height*hh),CornerRadius(4.dp.toPx()))
                    drawCircle(Color.White,4.dp.toPx(),Offset(size.width/2,size.height*(1-hh)))
                }; Text(l,fontSize=7.sp,color=AC.T3,fontFamily=FontFamily.Monospace)
            }}
        }
    }
    Asp("预设") { Aor(listOf("平坦","流行","摇滚","古典","低音增强"), "平坦") }
}

@Composable private fun FxPage() {
    Asp("音效") { Aor(listOf("无","混响","回声","降噪","压缩器"), "混响"); Asl("混响量","35",35f); Asl("房间","60",60f) }
    Asp("变速") { Asl("速度","1.0x",50f); Asl("音高","0",50f) }
    Asp("降噪") { Asl("强度","50",50f); Asl("阈值","-40dB",30f) }
}

@Composable private fun LibPage() {
    As("素材库") {
        Aor(listOf("全部","音乐","音效","环境"), "全部")
        Spacer(Modifier.height(8.dp))
        listOf("轻柔钢琴" to "02:15 · 免费", "Lo-Fi 节拍" to "03:42 · 免费",
            "史诗配乐" to "04:20 · PRO", "雨声白噪" to "10:00 · 免费").forEach { (n,d) ->
            val free = d.contains("免费")
            Row(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(8.dp)).background(AC.Card).padding(10.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(if(free)AC.Green.copy(0.1f)else AC.Gold.copy(0.1f)),
                    contentAlignment=Alignment.Center) { Text("▶",fontSize=10.sp,color=if(free)AC.Green else AC.Gold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(n,fontSize=11.sp,color=AC.T1,fontWeight=FontWeight.Medium); Text(d,fontSize=8.sp,color=AC.T3) }
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if(free)AC.Green.copy(0.12f)else AC.Gold.copy(0.12f)).padding(horizontal=6.dp,vertical=2.dp)) {
                    Text(if(free)"免费"else"PRO",fontSize=8.sp,color=if(free)AC.Green else AC.Gold,fontWeight=FontWeight.Bold) }
            }
        }
    }
}

// 组件
@Composable private fun As(t:String,c:@Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp)) {
    Text(t,fontSize=10.sp,color=AC.T3,fontWeight=FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); c() } }
@Composable private fun Asp(t:String,c:@Composable ColumnScope.()->Unit) { As(t,c); Spacer(Modifier.height(4.dp)); Box(Modifier.fillMaxWidth().padding(horizontal=12.dp).height(1.dp).background(AC.Line)); Spacer(Modifier.height(4.dp)) }
@Composable private fun Asl(l:String,v:String,p:Float,cs:List<Color>=listOf(AC.Acc,AC.Acc2)) { var pos by remember{mutableStateOf(p)}; Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically) {
    Text(l,fontSize=10.sp,color=AC.T2,modifier=Modifier.width(72.dp)); Canvas(Modifier.weight(1f).height(20.dp).pointerInput(Unit){detectDragGestures{c,_->c.consume();pos=(c.position.x/size.width*100).coerceIn(0f,100f)}}) {
    val cy=size.height/2;val w=size.width; drawRoundRect(AC.Card,Offset(0f,cy-2.dp.toPx()),Size(w,4.dp.toPx()),CornerRadius(2.dp.toPx())); drawRoundRect(Brush.linearGradient(cs),Offset(0f,cy-2.dp.toPx()),Size(w*pos/100,4.dp.toPx()),CornerRadius(2.dp.toPx())); drawCircle(AC.T1,5.dp.toPx(),Offset(w*pos/100,cy)) }; Text(v,fontSize=10.sp,color=AC.T3,modifier=Modifier.width(50.dp),textAlign=TextAlign.End) } }
@Composable private fun Aor(o:List<String>,s:String,onSel:(String)->Unit={}) { Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { o.forEach { val on=s==it; Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if(on)AC.Acc.copy(0.15f)else AC.Card).clickable{onSel(it)}.padding(horizontal=10.dp,vertical=5.dp)) {
    Text(it,fontSize=10.sp,color=if(on)AC.Acc else AC.T2) } } } }
