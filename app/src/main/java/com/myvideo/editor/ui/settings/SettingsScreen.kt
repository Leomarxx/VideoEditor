package com.myvideo.editor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object SC {
    val Bg = Color(0xFF080808); val Surf = Color(0xFF111111); val Card = Color(0xFF161616)
    val CardAlt = Color(0xFF1E1E1E); val Acc = Color(0xFF4A90D9); val Acc2 = Color(0xFF6EC850)
    val Gold = Color(0xFFE8A820); val Green = Color(0xFF6EC850); val Red = Color(0xFFE84848)
    val T1 = Color(0xFFF0ECE4); val T2 = Color(0xFFB0ACA4); val T3 = Color(0xFF6A6660)
    val Line = Color(0xFF1A1A1A); val Line2 = Color(0xFF242424)
}

data class SItem(val icon: String, val title: String, val sub: String, val value: String = "", val ac: Color = SC.Acc, val onClick: () -> Unit = {})
data class SGroup(val title: String, val items: List<SItem>)

@Composable
fun SettingsScreen(
    onOpenExportSettings: () -> Unit = {},
    onOpenAiModelManager: () -> Unit = {},
    onOpenPerformanceMonitor: () -> Unit = {},
    onOpenMemberCenter: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val groups = listOf(
        SGroup("工程设置", listOf(
            SItem("⚙", "渲染引擎", "选择渲染后端", "MediaCodec", SC.T2, onOpenExportSettings)
        )),
        SGroup("AI 模型", listOf(
            SItem("🧠", "模型管理", "查看和下载 AI 模型", "", SC.Acc, onOpenAiModelManager),
            SItem("⚡", "设备性能档位", "根据设备自动优化", "T1 旗舰", SC.Gold, onOpenPerformanceMonitor)
        )),
        SGroup("性能与缓存", listOf(
            SItem("🗑", "缓存管理", "清理缓存释放空间", "0 MB", SC.T2, onClearCache),
            SItem("📊", "性能中心", "内存 / 帧率 / 温度监控", "", SC.Acc2, onOpenPerformanceMonitor),
            SItem("🚀", "GPU 加速", "硬件加速渲染", "已开启", SC.Green)
        )),
        SGroup("通用", listOf(
            SItem("🌐", "语言", "界面语言", "中文", SC.Acc),
            SItem("🎨", "主题", "深色 / 浅色 / 跟随系统", "深色", SC.Acc),
            SItem("🔔", "通知设置", "导出完成提醒、更新提醒", "", SC.Acc),
            SItem("💾", "自动保存", "保存间隔", "30 秒", SC.Acc2),
            SItem("📁", "存储路径", "素材和缓存存储位置", "/内部存储", SC.T2)
        ))
    )
    val aboutGroup = SGroup("关于", listOf(
        SItem("ℹ", "版本信息", "当前版本", "v1.0.0", SC.T3, onOpenAbout),
        SItem("🏢", "归属权", "© 2025 NexClip Team", "", SC.T3),
        SItem("🤖", "模型声明", "AI 模型来源及许可说明", "", SC.T3, onOpenLicenses),
        SItem("📜", "开源许可", "第三方库许可证", "", SC.T2, onOpenLicenses),
        SItem("🔒", "隐私政策", "数据收集与使用说明", "", SC.T2, onOpenPrivacy),
        SItem("📋", "用户协议", "服务条款", "", SC.T2, onOpenTerms),
        SItem("💬", "意见反馈", "提交问题或建议", "", SC.Acc),
        SItem("🔄", "检查更新", "检查新版本", "", SC.Acc2),
        SItem("👥", "关于我们", "团队介绍", "", SC.T2, onOpenAbout),
        SItem("❓", "常见问题", "FAQ", "", SC.T2),
        SItem("🔗", "官方社群", "微信群 / QQ 群入口", "", SC.Acc)
    ))

    Column(modifier = Modifier.fillMaxSize().background(SC.Bg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SC.T1, letterSpacing = (-0.5).sp)
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            item {
                AccountCard(onOpenMemberCenter)
                Spacer(modifier = Modifier.height(8.dp))
            }
            (groups + aboutGroup).forEach { group ->
                item {
                    Text(group.title, fontSize = 9.sp, color = SC.T3, letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp))
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(SC.Card).padding(2.dp)) {
                        group.items.forEachIndexed { idx, item ->
                            SettingRow(item)
                            if (idx < group.items.lastIndex) {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                                    .height(0.5.dp).background(SC.Line))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            item {
                Text("NexClip · AI 智能视频编辑器", fontSize = 9.sp, color = SC.T3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AccountCard(onMember: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SC.Card)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SC.Acc, SC.Acc2))),
                    contentAlignment = Alignment.Center) {
                    Text("N", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("NexClip 用户", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SC.T1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(SC.Gold.copy(0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("免费版", fontSize = 9.sp, color = SC.Gold, fontWeight = FontWeight.SemiBold)
                        }
                        Text("  ID: 100001", fontSize = 9.sp, color = SC.T3,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(SC.Gold.copy(0.2f), SC.Gold.copy(0.08f))))
                .border(1.dp, SC.Gold.copy(0.3f), RoundedCornerShape(8.dp))
                .clickable { onMember() },
                contentAlignment = Alignment.Center) {
                Text("升级 PRO · 解锁全部功能", fontSize = 12.sp, color = SC.Gold, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingRow(item: SItem) {
    Row(modifier = Modifier.fillMaxWidth().clickable { item.onClick() }
        .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
            .background(item.ac.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Text(item.icon, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SC.T1)
            Text(item.sub, fontSize = 9.sp, color = SC.T3, modifier = Modifier.padding(top = 2.dp))
        }
        if (item.value.isNotEmpty()) {
            Text(item.value, fontSize = 10.sp, color = SC.T2, fontFamily = FontFamily.Monospace,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SC.CardAlt)
                    .padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("›", fontSize = 16.sp, color = SC.T3)
    }
}
