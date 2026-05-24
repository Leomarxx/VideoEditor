package com.myvideo.editor.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.theme.AppColors
import com.myvideo.editor.theme.AppTypography

data class TemplateItem(
    val id: String,
    val name: String,
    val category: String,
    val isPro: Boolean,
    val previewColors: List<Color>
)

@Composable
fun TemplateCenterScreen(
    templates: List<TemplateItem>,
    onBack: () -> Unit = {},
    onApplyTemplate: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("文字标题", "转场", "特效", "字幕样式")

    Column(modifier = Modifier.fillMaxSize().background(AppColors.BgPrimary)) {
        // 顶部
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("返回", color = AppColors.Accent, fontSize = 12.sp,
                modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("模板中心", style = AppTypography.HeadingMedium.copy(color = AppColors.TextPrimary))
        }

        // 标签页
        Row(modifier = Modifier.fillMaxWidth().height(36.dp).background(AppColors.BgSurface)
            .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabs.forEachIndexed { index, tab ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == index) AppColors.BgCard else Color.Transparent)
                    .clickable { selectedTab = index }, contentAlignment = Alignment.Center) {
                    Text(tab, fontSize = 10.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selectedTab == index) AppColors.Accent else AppColors.TextTertiary)
                }
            }
        }

        // 模板网格
        val filtered = templates.filter { it.category == tabs[selectedTab] }
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无模板", color = AppColors.TextDisabled, fontSize = 12.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp, 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { template ->
                    TemplateCard(template, onClick = { onApplyTemplate(template.id) })
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: TemplateItem, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.BgCard)
        .clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp)
            .background(Brush.linearGradient(template.previewColors)),
            contentAlignment = Alignment.Center) {
            Text(template.name, fontSize = 14.sp, color = Color.White.copy(alpha = .7f), fontWeight = FontWeight.Bold)
            if (template.isPro) {
                Text("PRO", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .clip(RoundedCornerShape(3.dp)).background(AppColors.Gold).padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
        Text(template.name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary,
            modifier = Modifier.padding(8.dp, 6.dp))
    }
}
