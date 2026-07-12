package com.filmatube.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Forced-dark green color scheme. Filmatube does not follow the system light/dark
 * setting and does not use Material You dynamic color — the brand look is always this.
 */
private val FilmatubeDarkColorScheme = darkColorScheme(
    primary = FilmatubeGreen,
    onPrimary = OnFilmatubeGreen,
    primaryContainer = FilmatubeGreenContainer,
    onPrimaryContainer = OnFilmatubeGreenContainer,
    secondary = FilmatubeSecondary,
    onSecondary = OnFilmatubeSecondary,
    secondaryContainer = FilmatubeSecondaryContainer,
    onSecondaryContainer = OnFilmatubeSecondaryContainer,
    tertiary = FilmatubeGold,
    onTertiary = OnFilmatubeGold,
    tertiaryContainer = FilmatubeGoldContainer,
    onTertiaryContainer = OnFilmatubeGoldContainer,
    error = FilmatubeError,
    onError = OnFilmatubeError,
    errorContainer = FilmatubeErrorContainer,
    onErrorContainer = OnFilmatubeErrorContainer,
    background = FilmatubeBackground,
    onBackground = OnFilmatubeBackground,
    surface = FilmatubeSurface,
    onSurface = OnFilmatubeSurface,
    surfaceVariant = FilmatubeSurfaceVariant,
    onSurfaceVariant = OnFilmatubeSurfaceVariant,
    outline = FilmatubeOutline,
    outlineVariant = FilmatubeOutlineVariant,
    inverseSurface = FilmatubeInverseSurface,
    inverseOnSurface = FilmatubeInverseOnSurface,
    inversePrimary = FilmatubeInversePrimary,
    scrim = FilmatubeScrim,
    surfaceTint = FilmatubeGreen,
    surfaceDim = FilmatubeSurfaceDim,
    surfaceBright = FilmatubeSurfaceBright,
    surfaceContainerLowest = FilmatubeSurfaceContainerLowest,
    surfaceContainerLow = FilmatubeSurfaceContainerLow,
    surfaceContainer = FilmatubeSurfaceContainer,
    surfaceContainerHigh = FilmatubeSurfaceContainerHigh,
    surfaceContainerHighest = FilmatubeSurfaceContainerHighest,
)

@Composable
fun FilmatubeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            // Dark background → light (white) status & navigation bar icons.
            insets.isAppearanceLightStatusBars = false
            insets.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = FilmatubeDarkColorScheme,
        typography = FilmatubeTypography,
        shapes = FilmatubeShapes,
    ) {
        // Root Surface sets the base background AND the app-wide content color
        // (LocalContentColor). Without it, screens that aren't wrapped in their own
        // Surface/Scaffold fall back to the default black content color.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
