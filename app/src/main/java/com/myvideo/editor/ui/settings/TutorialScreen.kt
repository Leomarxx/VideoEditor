package com.myvideo.editor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.myvideo.editor.theme.AppColors
import com.myvideo.editor.theme.AppTypography

data class TutorialItem(val title: String, val steps: List<String>, val tip: String? = null, val isAdvanced: Boolean = false)

private fun getTutorials() = listOf(
    TutorialItem("创建第一个项目", listOf("在首页点击新建项目按钮", "选择分辨率和帧率（推荐1080p 30fps）", "点击确认创建项目"), "首次使用建议选择1080p分辨率"),
    TutorialItem("导入素材", listOf("进入剪辑工作区后点击底部导入按钮", "选择手机中的视频/音频/图片文件", "素材会自动添加到时间轴上")),
    TutorialItem("剪辑素材", listOf("点击时间轴上的素材进行选中", "拖动素材两端手柄进行修剪", "长按素材可以拖拽移动位置", "点击分割按钮在播放头位置分割"), "双指缩放时间轴可以精确调整"),
    TutorialItem("添加转场效果", listOf("点击两个素材之间的连接处", "选择转场效果类型", "调整转场时长（建议0.5-1秒）")),
    TutorialItem("添加字幕", listOf("点击工具栏的字幕按钮", "输入字幕文字内容", "拖动字幕位置和时长", "调整字体、颜色、动画效果"), "可使用AI语音转文字自动生成字幕"),
    TutorialItem("调整音频", listOf("选中音频轨道", "使用音量滑块调整大小", "设置淡入淡出效果", "如需降噪可开启AI降噪")),
    TutorialItem("色彩调整", listOf("切换到底部调色面板", "使用三向色轮调整色调", "使用曲线面板精确控制", "调整色温改变画面氛围"), isAdvanced = true),
    TutorialItem("导出视频", listOf("点击右上角导出按钮", "选择分辨率帧率编码格式", "选择码率（推荐自动）", "点击开始导出"), "4K和高帧率导出需要会员权限", isAdvanced = true)
)

@Composable
fun TutorialScreen(onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }
    var expandedIndex by remember { mutableStateOf(-1) }
    Column(modifier = Modifier.fillMaxSize().background(AppColors.BgPrimary)) {
        Row(modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("返回", color = AppColors.Accent, fontSize = 12.sp, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("使用教程", style = AppTypography.HeadingMedium.copy(color = AppColors.TextPrimary))
        }
        Row(modifier = Modifier.fillMaxWidth().height(36.dp).background(AppColors.BgSurface)) {
            listOf("基础教程", "进阶教程").forEachIndexed { index, tab ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedTab = index }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selectedTab == index) AppColors.Accent else AppColors.TextTertiary)
                        if (selectedTab == index) Box(modifier = Modifier.padding(top = 4.dp).width(20.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(AppColors.Accent))
                    }
                }
            }
        }
        val tutorials = getTutorials().filter { if (selectedTab == 0) !it.isAdvanced else it.isAdvanced }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(tutorials) { index, tutorial ->
                val globalIndex = getTutorials().indexOf(tutorial)
                TutorialCard(tutorial, expandedIndex == globalIndex, { expandedIndex = if (expandedIndex == globalIndex) -1 else globalIndex })
            }
        }
    }
}

@Composable
private fun TutorialCard(tutorial: TutorialItem, isExpanded: Boolean, onToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.BgCard)) {
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.BgCardAlt), contentAlignment = Alignment.Center) {
                Text(tutorial.title.first().toString(), fontSize = 22.sp, color = AppColors.Accent)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tutorial.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text("${tutorial.steps.size}个步骤", fontSize = 9.sp, color = AppColors.TextTertiary)
                    if (tutorial.isAdvanced) Text("进阶", fontSize = 7.sp, color = AppColors.Gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x33E8A820)).padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Text(if (isExpanded) "▼" else "▶", fontSize = 12.sp, color = AppColors.TextDisabled)
        }
        if (isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("← 返回", fontSize = 11.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 12.dp).clickable { onToggle() })
                tutorial.steps.forEachIndexed { stepIndex, step ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(AppColors.Accent), contentAlignment = Alignment.Center) {
                            Text("${stepIndex + 1}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(step, fontSize = 10.5.sp, color = AppColors.TextPrimary, lineHeight = 15.sp, modifier = Modifier.weight(1f).padding(top = 1.dp))
                    }
                }
                if (tutorial.tip != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp).background(Color(0x0F4A90D9)).padding(10.dp)) {
                        Text("提示", fontSize = 8.sp, color = AppColors.TextTertiary, fontWeight = FontWeight.Bold)
                        Text(tutorial.tip, fontSize = 10.sp, color = AppColors.TextSecondary, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
