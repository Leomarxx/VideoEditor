package com.myvideo.editor.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun currentLayoutMode(): LayoutMode {
    val config = LocalConfiguration.current
    return detectLayoutMode(config.screenWidthDp, config.screenHeightDp)
}

@Composable
fun isTablet(): Boolean = currentLayoutMode() == LayoutMode.TABLET

@Composable
fun isLandscape(): Boolean = currentLayoutMode() == LayoutMode.PHONE_LANDSCAPE
