package com.myvideo.editor.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myvideo.editor.engine.VideoImportManager
import com.myvideo.editor.engine.VideoPicker
import com.myvideo.editor.theme.AppColors
import com.myvideo.editor.ui.dashboard.DashboardScreen
import com.myvideo.editor.ui.editor.EditorScreen
import com.myvideo.editor.ui.editor.EditorViewModel
import com.myvideo.editor.ui.settings.*
import com.myvideo.editor.ui.color.ColorScreen

@Composable
fun NavGraph() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("dashboard") }
    var showTutorial by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val editorVm = remember { EditorViewModel() }
    val importManager = remember { VideoImportManager(context) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = VideoPicker.extractUri(result.data)
            if (uri != null) {
                val success = importManager.importToEditor(uri, editorVm)
                if (success) currentTab = "editor"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.BgPrimary)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                showPrivacy -> PrivacyScreen(onBack = { showPrivacy = false })
                showTerms -> TermsScreen(onBack = { showTerms = false })
                showAbout -> AboutScreen(
                    onPrivacyPolicy = { showPrivacy = true; showAbout = false },
                    onTerms = { showTerms = true; showAbout = false },
                    onBack = { showAbout = false }
                )
                showTutorial -> TutorialScreen(onBack = { showTutorial = false })
                currentTab == "dashboard" -> DashboardScreen(
                    recentProjects = emptyList(),
                    allProjects = emptyList(),
                    onCreateProject = { videoPickerLauncher.launch(VideoPicker.getPickIntent()) },
                    onOpenProject = { currentTab = "editor" },
                    onOpenDraftBox = {},
                    onOpenTemplateCenter = {},
                    onOpenTutorial = { showTutorial = true },
                    onSearchProject = {},
                    onRenameProject = {},
                    onDeleteProject = {},
                    onDuplicateProject = {}
                )
                currentTab == "editor" -> EditorScreen(vm = editorVm)
                currentTab == "color" -> ColorScreen(onBack = { currentTab = "dashboard" })
                currentTab == "audio" -> PagePlaceholder("音频编辑")
                currentTab == "settings" -> SettingsScreen(
                    onOpenExportSettings = {},
                    onOpenAiModelManager = {},
                    onOpenPerformanceMonitor = {},
                    onOpenMemberCenter = {},
                    onOpenTutorial = { showTutorial = true },
                    onOpenLicenses = {},
                    onClearCache = {},
                    onOpenPrivacy = { showPrivacy = true },
                    onOpenTerms = { showTerms = true },
                    onOpenAbout = { showAbout = true }
                )
            }
        }

        if (!showTutorial && !showPrivacy && !showTerms && !showAbout) {
            Row(modifier = Modifier.fillMaxWidth().height(60.dp).background(AppColors.BgSurface),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically) {
                val tabs = listOf("dashboard" to "首页", "editor" to "剪辑", "color" to "调色", "audio" to "音频", "settings" to "设置")
                tabs.forEach { (route, label) ->
                    val isSelected = currentTab == route
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { currentTab = route }.padding(vertical = 8.dp)) {
                        Text(label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AppColors.Accent else AppColors.TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagePlaceholder(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, color = AppColors.TextPrimary, fontSize = 20.sp)
    }
}
