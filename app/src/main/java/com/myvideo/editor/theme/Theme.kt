package com.myvideo.editor.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val BgPrimary = Color(0xFF080808)
    val BgSurface = Color(0xFF111111)
    val BgCard = Color(0xFF161616)
    val BgCardAlt = Color(0xFF1E1E1E)

    val Accent = Color(0xFF4A90D9)
    val Gold = Color(0xFFE8A820)
    val Red = Color(0xFFE85050)

    val TextPrimary = Color(0xFFF0ECE4)
    val TextSecondary = Color(0xFFB0ACA4)
    val TextTertiary = Color(0xFF6A6660)
    val TextDisabled = Color(0xFF3A3A3A)
}

object AppTypography {
    val HeadingLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    val HeadingMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
    val HeadingSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
    val BodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = AppColors.TextSecondary)
    val BodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = AppColors.TextSecondary)
    val BodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = AppColors.TextTertiary)
    val Caption = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = AppColors.TextTertiary)
}
