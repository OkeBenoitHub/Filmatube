package com.filmatube.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Filmatube typography. Uses the platform default font for now; a custom brand font
 * can be dropped into `res/font` and swapped here later without touching call sites.
 */
private val DefaultFontFamily = FontFamily.Default

val FilmatubeTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.merge(
            TextStyle(fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold),
        ),
        headlineMedium = headlineMedium.merge(
            TextStyle(fontFamily = DefaultFontFamily, fontWeight = FontWeight.Bold),
        ),
        titleLarge = titleLarge.merge(
            TextStyle(fontFamily = DefaultFontFamily, fontWeight = FontWeight.SemiBold),
        ),
        titleMedium = titleMedium.merge(
            TextStyle(fontFamily = DefaultFontFamily, fontWeight = FontWeight.SemiBold),
        ),
        labelLarge = labelLarge.merge(
            TextStyle(fontFamily = DefaultFontFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
        ),
        bodyLarge = bodyLarge.merge(TextStyle(fontFamily = DefaultFontFamily)),
        bodyMedium = bodyMedium.merge(TextStyle(fontFamily = DefaultFontFamily)),
    )
}
