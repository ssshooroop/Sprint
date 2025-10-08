package com.sprint.runner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// Wear OS Material Color Palette
private val wearColorPalette = Colors(
    primary = Color(0xFF1a73e8),
    primaryVariant = Color(0xFF0d47a1),
    secondary = Color(0xFF34a853),
    secondaryVariant = Color(0xFF1e8e3e),
    error = Color(0xFFea4335),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onError = Color.White
)

@Composable
fun SprintRunnerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = wearColorPalette,
        typography = Typography,
        content = content
    )
}