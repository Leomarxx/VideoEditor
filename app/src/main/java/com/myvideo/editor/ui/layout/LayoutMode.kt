package com.myvideo.editor.ui.layout

enum class LayoutMode { PHONE_PORTRAIT, PHONE_LANDSCAPE, TABLET }

fun detectLayoutMode(widthDp: Int, heightDp: Int): LayoutMode {
    return when {
        widthDp > 840 -> LayoutMode.TABLET
        widthDp > heightDp -> LayoutMode.PHONE_LANDSCAPE
        else -> LayoutMode.PHONE_PORTRAIT
    }
}
