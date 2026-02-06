package io.github.numq.haskcore.platform.ui.window.decoration

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class WindowDecorationColors(
    val decoration: @Composable () -> Color = {
        MaterialTheme.colorScheme.surface
    },
    val title: @Composable () -> Color = {
        MaterialTheme.colorScheme.onSurface
    },
    val minimizeButton: @Composable () -> Color = {
        MaterialTheme.colorScheme.primary
    },
    val fullscreenButton: @Composable () -> Color = {
        MaterialTheme.colorScheme.primary
    },
    val closeButton: @Composable () -> Color = {
        MaterialTheme.colorScheme.primary
    },
    val content: @Composable () -> Color = {
        MaterialTheme.colorScheme.background
    },
)