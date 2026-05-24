package com.myvideo.editor.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.theme.AppColors
import com.myvideo.editor.theme.AppTypography
import kotlinx.coroutines.delay

data class ProjectItem(
    val id: String, val name: String, val duration: String,
    val resolution: String, val lastModified: String,
    val thumbnailColors: List<Color>, val isDraft: Boolean = false
)

data class DraftItem(
    val id: String, val name: String, val duration: String,
    val lastSaved: String, val thumbnailColors: List<Color>
)

private object DC {
    val Bg = Color(0xFF1E1E1E); val Surf = Color(0xFF282828)
    val Card = Color(0xFF2C2C2C); val CardH = Color(0xFF323232)
    val Line = Color(0xFF3A3A3A); val Line2 = Color(0xFF444444)
    val T1 = Color(0xFFCCCCCC); val T2 = Color(0xFF999999)
    val T3 = Color(0xFF666666); val T4 = Color(0xFF4A4A4A)
    val Acc = Color(0xFF4A90D9); val AccL = Color(0xFF6AAFE6)
    val AccS = Color(0x1F4A90D9); val Gold = Color(0xFFE8A820)
    val Green = Color(0xFF7EC850); val Red = Color(0xFFE85050)
}

@Composable
fun DashboardScreen(
    recentProjects: List<ProjectItem> = emptyList(),
    allProjects: List<ProjectItem> = emptyList(),
    onCreateProject: () -> Unit = {},
    onOpenProject: (String) -> Unit = {},
    onOpenDraftBox: () -> Unit = {},
    onOpenTemplateCenter: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    onSearchProject: (String) -> Unit = {},
    onRenameProject: (String, String) -> Unit = { _, _ -> },
    onDeleteProject: (String) -> Unit = {},
    onDuplicateProject: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showModal by remember { mutableStateOf(false) }
    var ctxTarget by remember { mutableStateOf<ProjectItem?>(null) }
    var showCtx by remember { mutableStateOf(false) }
    var ctxPos by remember { mutableStateOf(Offset.Zero) }
    var toast by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    LaunchedEffect(showToast) { if (showToast) { delay(2000); showToast = false } }

    Box(modifier = Modifier.fillMaxSize().background(DC.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("项目", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = DC.T1))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(DC.Card)
                        .clickable { showSearch = !showSearch }, contentAlignment = Alignment.Center) {
                        Text("搜", fontSize = 10.sp, color = DC.T3)
                    }
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(DC.Card)
                        .clickable { onOpenSettings() }, contentAlignment = Alignment.Center) {
                        GearIcon()
                    }
                }
            }

            AnimatedVisibility(visible = showSearch, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
                val fr = remember { androidx.compose.ui.focus.FocusRequester() }
                LaunchedEffect(Unit) { delay(100); fr.requestFocus() }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(38.dp)
                    .clip(RoundedCornerShape(10.dp)).background(DC.Card).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("搜", fontSize = 12.sp, color = DC.T3)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(value = searchQuery, onValueChange = { searchQuery = it; onSearchProject(it) },
                            modifier = Modifier.weight(1f).focusRequester(fr),
                            textStyle = TextStyle(fontSize = 12.sp, color = DC.T1), singleLine = true,
                            cursorBrush = SolidColor(DC.T3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✕", fontSize = 14.sp, color = DC.T3, modifier = Modifier.clickable {
                            searchQuery = ""; onSearchProject(""); showSearch = false
                        })
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (showSearch) 0.dp else 16.dp)
                .height(48.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(DC.Acc, DC.AccL)))
                .clickable { showModal = true }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("开始创建项目", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Text("草稿箱", style = TextStyle(fontSize = 11.sp, color = DC.T4, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentProjects.filter { it.isDraft }) { p ->
                    DraftCard(DraftItem(p.id, p.name, p.duration, p.lastModified, p.thumbnailColors)) { onOpenProject(p.id) }
                }
            }

            Text("全部项目", style = TextStyle(fontSize = 11.sp, color = DC.T4, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 10.dp))
            val filtered = if (searchQuery.isEmpty()) allProjects else allProjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "还没有项目，点击上方按钮新建" else "没有找到匹配的项目", color = DC.T4, fontSize = 12.sp)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(filtered) { p ->
                        ProjectListItem(p, { onOpenProject(p.id) }, { o -> ctxTarget = p; ctxPos = o; showCtx = true })
                    }
                }
            }
        }

        Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp)
            .background(DC.Surf).border(1.dp, DC.Line),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            NavItem("项目", true) {}
            NavItem("教程素材", false) { onOpenTutorial() }
            NavItem("设置", false) { onOpenSettings() }
        }

        if (showModal) CreateModal(onDismiss = { showModal = false }) { name, _, _, _ ->
            onCreateProject(); showModal = false; toast = "项目 \"$name\" 已创建"; showToast = true
        }

        if (showCtx && ctxTarget != null) {
            Box(modifier = Modifier.fillMaxSize().clickable { showCtx = false }) {
                Column(modifier = Modifier.offset((ctxPos.x / 2).dp, (ctxPos.y / 2).dp)
                    .width(130.dp).clip(RoundedCornerShape(10.dp)).background(DC.CardH)
                    .border(1.dp, DC.Line2, RoundedCornerShape(10.dp)).padding(4.dp)) {
                    CtxItem("重命名", false) { ctxTarget?.let { onRenameProject(it.id, it.name) }; showCtx = false }
                    CtxItem("复制项目", false) { ctxTarget?.let { onDuplicateProject(it.id) }; showCtx = false; toast = "已复制项目"; showToast = true }
                    CtxItem("删除项目", true) { ctxTarget?.let { onDeleteProject(it.id) }; showCtx = false; toast = "已删除"; showToast = true }
                }
            }
        }

        if (showToast) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                .clip(RoundedCornerShape(10.dp)).background(DC.CardH).border(1.dp, DC.Line2, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(toast, fontSize = 11.sp, color = DC.T1)
            }
        }
    }
}

@Composable
private fun DraftCard(draft: DraftItem, onClick: () -> Unit) {
    Box(modifier = Modifier.width(160.dp).height(100.dp).clip(RoundedCornerShape(14.dp))
        .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(draft.thumbnailColors)))
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)), startY = 60f)))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp, 6.dp)) {
            Text(draft.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("${draft.duration} · ${draft.lastSaved}", fontSize = 9.sp, color = DC.T2, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ProjectListItem(project: ProjectItem, onClick: () -> Unit, onLongClick: (Offset) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DC.Card)
        .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { o -> onLongClick(o) }) }
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(project.thumbnailColors)),
            contentAlignment = Alignment.Center) { Text("▶", fontSize = 16.sp, color = Color.White.copy(alpha = 0.5f)) }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DC.T1, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (project.isDraft) Text("草稿", fontSize = 7.sp, color = DC.Gold, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(3.dp)).background(DC.AccS).padding(horizontal = 4.dp, vertical = 1.dp))
            }
            Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(project.duration, fontSize = 9.sp, color = DC.T3, fontFamily = FontFamily.Monospace)
                Text(project.resolution, fontSize = 9.sp, color = DC.T3, fontFamily = FontFamily.Monospace)
            }
        }
        Text(project.lastModified, fontSize = 9.sp, color = DC.AccL, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(DC.AccS).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Text(label, fontSize = 12.sp, color = if (selected) DC.Acc else DC.T3, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, fontSize = 9.sp, color = if (selected) DC.Acc else DC.T3, fontWeight = FontWeight.Medium)
        if (selected) { Spacer(modifier = Modifier.height(4.dp)); Box(modifier = Modifier.width(20.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(DC.Acc)) }
    }
}

@Composable
private fun CtxItem(label: String, danger: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).clickable { onClick() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (danger) DC.Red else DC.T2)
    }
}

@Composable
private fun GearIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val c = size.width / 2
        val outerR = size.width * 0.35f
        val innerR = size.width * 0.15f
        val color = Color(0xFF666666)
        val strokeW = size.width * 0.07f
        // 外圈
        drawCircle(color = color, radius = outerR, center = Offset(c, c), style = Stroke(strokeW))
        // 内圈
        drawCircle(color = color, radius = innerR, center = Offset(c, c), style = Stroke(strokeW))
        // 齿轮齿（8个）
        val teeth = 8
        val toothLen = size.width * 0.1f
        val toothW = size.width * 0.08f
        for (i in 0 until teeth) {
            val angle = Math.toRadians((i * 360.0 / teeth)).toFloat()
            val startR = outerR - strokeW / 2
            val endR = outerR + toothLen
            val sx = c + startR * kotlin.math.cos(angle)
            val sy = c + startR * kotlin.math.sin(angle)
            val ex = c + endR * kotlin.math.cos(angle)
            val ey = c + endR * kotlin.math.sin(angle)
            drawLine(color = color, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = toothW)
        }
    }
}

@Composable
private fun CreateModal(onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("未命名项目") }
    var ratio by remember { mutableStateOf("16:9") }
    var res by remember { mutableStateOf("3840×2160") }
    var bg by remember { mutableStateOf("#2a2a2a,#1a1a1a") }
    val ratios = listOf("16:9" to "横屏", "9:16" to "竖屏", "1:1" to "方形", "4:3" to "传统", "21:9" to "超宽")
    val resolutions = listOf("3840×2160" to "4K", "2560×1440" to "2K", "1920×1080" to "1080P", "1280×720" to "720P")
    val bgs = listOf("#2a2a2a,#1a1a1a", "#1a2030,#0a1520", "#2a1a2a,#1a0a1a", "#1a2a1a,#0a1a0a", "#2a2a1a,#1a1a0a", "#0a1a2a,#050f1a", "#000000", "#ffffff")

    Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(340.dp).clip(RoundedCornerShape(16.dp)).background(DC.Surf)
            .border(1.dp, DC.Line2, RoundedCornerShape(16.dp)).clickable { }) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp, 18.dp, 18.dp, 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("新建项目", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DC.T1)
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(DC.Card).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 14.sp, color = DC.T3)
                }
            }
            Column(modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 20.dp)) {
                Text("项目名称", fontSize = 10.sp, color = DC.T3, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().height(36.dp)
                    .clip(RoundedCornerShape(8.dp)).background(DC.Card).border(1.dp, DC.Line2, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
                    textStyle = TextStyle(fontSize = 12.sp, color = DC.T1), singleLine = true, cursorBrush = SolidColor(DC.T3))
                Spacer(modifier = Modifier.height(14.dp))
                Text("画面比例", fontSize = 10.sp, color = DC.T3, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ratios.forEach { (r, l) ->
                        val sel = ratio == r
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (sel) DC.AccS else DC.Card).border(1.5.dp, if (sel) DC.Acc else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { ratio = r }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(r, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (sel) DC.AccL else DC.T2)
                                Text(l, fontSize = 8.sp, color = if (sel) DC.AccL else DC.T3, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("分辨率", fontSize = 10.sp, color = DC.T3, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    resolutions.forEach { (r, l) ->
                        val sel = res == r
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (sel) DC.AccS else DC.Card).border(1.5.dp, if (sel) DC.Acc else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { res = r }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(l, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (sel) DC.AccL else DC.T2)
                                Text(r, fontSize = 8.sp, color = if (sel) DC.AccL else DC.T3, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("项目背景", fontSize = 10.sp, color = DC.T3, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bgs.forEach { b ->
                        val sel = bg == b
                        val colors = b.split(",").map { c -> Color(android.graphics.Color.parseColor("#${c.trim()}")) }
                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(colors)).border(2.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { bg = b }, contentAlignment = Alignment.Center) {
                            if (sel) Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(DC.Acc, DC.AccL)))
                    .clickable { onCreate(name, ratio, res, bg) }, contentAlignment = Alignment.Center) {
                    Text("创建项目", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
