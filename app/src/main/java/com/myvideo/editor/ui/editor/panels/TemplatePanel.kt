package com.myvideo.editor.ui.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

data class ProjectTemplate(
    val name: String,
    val desc: String,
    val trackCount: Int,
    val clipPreviews: List<Color>
)

@Composable
fun TemplatePanel(vm: com.myvideo.editor.ui.editor.EditorViewModel = com.myvideo.editor.ui.editor.EditorViewModel(), onApply: (String) -> Unit = {}, onClose: () -> Unit = {}) {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val templates = listOf(
        ProjectTemplate("Vlog 模板", "旅行记录、日常分享", 2,
            listOf(Color(0x334A90D9), Color(0x334A90D9), Color(0x1A7EC850))),
        ProjectTemplate("产品展示", "产品宣传、电商视频", 2,
            listOf(Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9), Color(0x2EE8A820))),
        ProjectTemplate("卡点视频", "音乐节拍自动对齐", 1,
            listOf(Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9))),
        ProjectTemplate("短视频", "15秒抖音/快手", 2,
            listOf(Color(0x334A90D9), Color(0x2EE8A820), Color(0x2EE8A820))),
        ProjectTemplate("故事片", "多段叙事结构", 3,
            listOf(Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9), Color(0x2EE8A820), Color(0x1A7EC850))),
        ProjectTemplate("教程", "操作演示、教学", 2,
            listOf(Color(0x334A90D9), Color(0x334A90D9), Color(0x334A90D9), Color(0x2EE8A820)))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("选择模板", fontSize = 9.sp, color = CG.T4, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        templates.forEachIndexed { idx, tpl ->
            val sel = selectedIndex == idx
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CG.Card)
                .then(if (sel) Modifier.border(2.dp, CG.Acc, RoundedCornerShape(10.dp)) else Modifier.border(2.dp, Color.Transparent, RoundedCornerShape(10.dp)))
                .clickable { selectedIndex = idx }
                .padding(12.dp)) {
                Text(tpl.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CG.T1)
                Text(tpl.desc, fontSize = 9.sp, color = CG.T3, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tpl.clipPreviews.forEach { c ->
                        Box(modifier = Modifier.height(16.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(c))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ApplyButton("套用模板") {
            if (selectedIndex >= 0) {
                onApply(templates[selectedIndex].name)
                onClose()
            }
        }
    }
}
