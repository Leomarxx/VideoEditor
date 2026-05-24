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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SubtitleItem(
    val id: Int,
    val startTime: String,
    val endTime: String,
    val text: String,
    val style: String = "默认"
)

@Composable
fun SubtitleEditorScreen(onBack: () -> Unit = {}) {
    var subtitles by remember { mutableStateOf(listOf(
        SubtitleItem(1, "00:00:01", "00:00:05", "这是第一句字幕"),
        SubtitleItem(2, "00:00:06", "00:00:10", "这是第二句字幕"),
        SubtitleItem(3, "00:00:12", "00:00:16", "第三句字幕内容")
    )) }
    var selectedIdx by remember { mutableIntStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080808)).padding(16.dp)) {
        // 顶栏
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("字幕编辑", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            // 添加按钮
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF4A90D9).copy(0.2f))
                .clickable {
                    val newId = (subtitles.maxOfOrNull { it.id } ?: 0) + 1
                    subtitles = subtitles + SubtitleItem(newId, "00:00:00", "00:00:03", "新字幕")
                }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("+ 添加", fontSize = 11.sp, color = Color(0xFF6AAFE6))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 字幕列表
        subtitles.forEachIndexed { idx, sub ->
            val sel = idx == selectedIdx
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(if (sel) Color(0xFF1A2A3A) else Color(0xFF141414))
                .clickable { selectedIdx = idx; editText = sub.text; isEditing = false }
                .padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${idx + 1}", fontSize = 10.sp, color = Color(0xFF666666), fontFamily = FontFamily.Monospace)
                        Text(sub.startTime, fontSize = 10.sp, color = Color(0xFF4A90D9), fontFamily = FontFamily.Monospace)
                        Text("→", fontSize = 10.sp, color = Color(0xFF666666))
                        Text(sub.endTime, fontSize = 10.sp, color = Color(0xFF4A90D9), fontFamily = FontFamily.Monospace)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("编辑", fontSize = 10.sp, color = Color(0xFF4A90D9), modifier = Modifier.clickable { isEditing = true; editText = sub.text })
                        Text("删除", fontSize = 10.sp, color = Color(0xFFE85050), modifier = Modifier.clickable {
                            subtitles = subtitles.toMutableList().also { it.removeAt(idx) }
                        })
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (sel && isEditing) {
                    // 编辑模式
                    Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF222222)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Text(editText, fontSize = 13.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF4A90D9).copy(0.2f))
                            .clickable {
                                subtitles = subtitles.toMutableList().also { it[idx] = sub.copy(text = editText) }
                                isEditing = false
                            }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("保存", fontSize = 10.sp, color = Color(0xFF6AAFE6))
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF333333))
                            .clickable { isEditing = false }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("取消", fontSize = 10.sp, color = Color(0xFF999999))
                        }
                    }
                } else {
                    Text(sub.text, fontSize = 13.sp, color = Color(0xFFCCCCCC))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (subtitles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Text("暂无字幕，点击右上角添加", fontSize = 12.sp, color = Color(0xFF666666))
            }
        }
    }
}
