package com.github.jimmy90109.livestatus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.lerp

@Composable
fun LiveStatusTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val oppositeColors = if (darkTheme) LightAppColors else DarkAppColors
    val baseColors = if (darkTheme) darkColorScheme() else lightColorScheme()
    // Supply every neutral role so Material components never fall back to the purple baseline.
    val materialColors = baseColors.copy(
        primary = appColors.commonPrimary,
        onPrimary = appColors.commonOnPrimary,
        primaryContainer = appColors.commonContainer,
        onPrimaryContainer = appColors.onSurface,
        inversePrimary = oppositeColors.commonPrimary,
        secondary = appColors.commonPrimary,
        onSecondary = appColors.commonOnPrimary,
        secondaryContainer = appColors.commonContainer,
        onSecondaryContainer = appColors.onSurface,
        tertiary = appColors.commonPrimary,
        onTertiary = appColors.commonOnPrimary,
        tertiaryContainer = appColors.commonContainer,
        onTertiaryContainer = appColors.onSurface,
        background = appColors.background,
        onBackground = appColors.onSurface,
        surface = appColors.commonSurface,
        onSurface = appColors.onSurface,
        surfaceVariant = appColors.commonContainer,
        onSurfaceVariant = appColors.onSurfaceVariant,
        surfaceTint = appColors.commonPrimary,
        inverseSurface = oppositeColors.commonSurface,
        inverseOnSurface = oppositeColors.onSurface,
        outline = appColors.onSurfaceVariant,
        outlineVariant = appColors.commonContainer,
        surfaceBright = appColors.background,
        surfaceDim = appColors.commonContainer,
        surfaceContainer = appColors.commonSurface,
        surfaceContainerLow = appColors.background,
        surfaceContainerLowest = appColors.background,
        surfaceContainerHigh = appColors.commonContainer,
        surfaceContainerHighest = appColors.commonContainer,
        primaryFixed = LightAppColors.commonContainer,
        primaryFixedDim = lerp(LightAppColors.commonContainer, LightAppColors.commonPrimary, 0.15f),
        onPrimaryFixed = LightAppColors.onSurface,
        onPrimaryFixedVariant = LightAppColors.onSurfaceVariant,
        secondaryFixed = LightAppColors.commonContainer,
        secondaryFixedDim = lerp(LightAppColors.commonContainer, LightAppColors.commonPrimary, 0.15f),
        onSecondaryFixed = LightAppColors.onSurface,
        onSecondaryFixedVariant = LightAppColors.onSurfaceVariant,
        tertiaryFixed = LightAppColors.commonContainer,
        tertiaryFixedDim = lerp(LightAppColors.commonContainer, LightAppColors.commonPrimary, 0.15f),
        onTertiaryFixed = LightAppColors.onSurface,
        onTertiaryFixedVariant = LightAppColors.onSurfaceVariant,
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content,
        )
    }
}
