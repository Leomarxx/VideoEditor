package com.myvideo.editor.ui.subtitle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtitleBatchEditor(
    subtitles: List<SubtitleItem> = emptyList(),
    onBatchAction: (String, List<Int>) -> Unit = { _, _ -> }
) {
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var selectAll by remember { mutableStateOf(false) }
    var targetFont by remember { mutableStateOf("默认") }
    var targetColor by remember { mutableStateOf("#FFFFFF") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 全选/取消
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("批量编辑", fontSize = 11.sp, color = Color(0xFF999999), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A))
                    .clickable {
                        selectAll = !selectAll
                        selectedIds = if (selectAll) subtitles.map { it.id }.toSet() else emptySet()
                    }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(if (selectAll) "取消全选" else "全选", fontSize = 10.sp, color = Color(0xFFCCCCCC))
                }
                Text("已选 ${selectedIds.size}/${subtitles.size}", fontSize = 10.sp, color = Color(0xFF666666))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 字幕选择列表
        subtitles.forEach { sub ->
            val sel = selectedIds.contains(sub.id)
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                .background(if (sel) Color(0xFF1A2A3A) else Color.Transparent)
                .clickable {
                    selectedIds = if (sel) selectedIds - sub.id else selectedIds + sub.id
                }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (sel) Color(0xFF4A90D9) else Color(0xFF333333)),
                    contentAlignment = Alignment.Center) {
                    if (sel) Text("✓", fontSize = 10.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("${sub.id}", fontSize = 10.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.width(6.dp))
                Text(sub.text, fontSize = 12.sp, color = Color(0xFFCCCCCC), maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 批量操作
        if (selectedIds.isNotEmpty()) {
            Text("批量操作", fontSize = 10.sp, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("统一字体", "统一颜色", "统一样式").forEach { action ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable { onBatchAction(action, selectedIds.toList()) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(action, fontSize = 10.sp, color = Color(0xFFCCCCCC))
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE85050).copy(0.15f))
                    .clickable { onBatchAction("删除", selectedIds.toList()) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("批量删除", fontSize = 10.sp, color = Color(0xFFE85050))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF6EC850).copy(0.15f))
                    .clickable { onBatchAction("导出SRT", selectedIds.toList()) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("导出SRT", fontSize = 10.sp, color = Color(0xFF6EC850))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF4A90D9).copy(0.15f))
                    .clickable { onBatchAction("导出ASS", selectedIds.toList()) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("导出ASS", fontSize = 10.sp, color = Color(0xFF6AAFE6))
                }
            }
        }
    }
}
