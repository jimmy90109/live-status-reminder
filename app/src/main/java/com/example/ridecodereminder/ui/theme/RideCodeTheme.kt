package com.example.ridecodereminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RideCodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(AppColors.BACKGROUND),
            surface = Color(AppColors.COMMON_SURFACE),
            onSurface = Color(AppColors.ON_SURFACE),
        ),
        content = content,
    )
}
