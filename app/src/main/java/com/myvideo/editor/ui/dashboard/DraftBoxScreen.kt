package com.myvideo.editor.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.theme.AppColors
import com.myvideo.editor.theme.AppTypography

data class DraftItem(
    val id: String,
    val name: String,
    val duration: String,
    val lastSaved: String,
    val autoSaved: Boolean,
    val thumbnailColors: List<Color>
)

@Composable
fun DraftBoxScreen(
    drafts: List<DraftItem>,
    onBack: () -> Unit = {},
    onOpenDraft: (String) -> Unit = {},
    onDeleteDraft: (String) -> Unit = {},
    onDeleteAll: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.BgPrimary)) {
        // 顶部
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("返回", color = AppColors.Accent, fontSize = 12.sp,
                    modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.width(12.dp))
                Text("草稿箱", style = AppTypography.HeadingMedium.copy(color = AppColors.TextPrimary))
            }
            if (drafts.isNotEmpty()) {
                Text("清空", color = AppColors.Red, fontSize = 11.sp,
                    modifier = Modifier.clickable { onDeleteAll() })
            }
        }

        // 草稿列表
        if (drafts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("草稿箱为空", fontSize = 16.sp, color = AppColors.TextDisabled)
                    Text("未完成的项目会自动保存到这里", fontSize = 11.sp, color = AppColors.TextTertiary,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(drafts) { draft ->
                    DraftCard(draft, onClick = { onOpenDraft(draft.id) }, onDelete = { onDeleteDraft(draft.id) })
                }
            }
        }
    }
}

@Composable
private fun DraftCard(draft: DraftItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.BgCard)
        .clickable { onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(draft.thumbnailColors)))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(draft.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (draft.autoSaved) {
                    Text("自动保存", fontSize = 7.sp, color = AppColors.Gold,
                        modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33E8A820)).padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
            Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(draft.duration, fontSize = 9.sp, color = AppColors.TextTertiary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text(draft.lastSaved, fontSize = 9.sp, color = AppColors.TextTertiary)
            }
        }
        Text("删除", fontSize = 9.sp, color = AppColors.Red,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x1AE85050))
                .clickable { onDelete() }.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
