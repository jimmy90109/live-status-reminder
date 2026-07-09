package com.github.jimmy90109.livestatus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun RideCodeTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = appColors.commonPrimary,
            onPrimary = appColors.commonOnPrimary,
            background = appColors.background,
            onBackground = appColors.onSurface,
            surface = appColors.commonSurface,
            onSurface = appColors.onSurface,
            onSurfaceVariant = appColors.onSurfaceVariant,
        )
    } else {
        lightColorScheme(
            primary = appColors.commonPrimary,
            onPrimary = appColors.commonOnPrimary,
            background = appColors.background,
            onBackground = appColors.onSurface,
            surface = appColors.commonSurface,
            onSurface = appColors.onSurface,
            onSurfaceVariant = appColors.onSurfaceVariant,
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content,
        )
    }
}
