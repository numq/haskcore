package io.github.numq.haskcore.platform.ui.window.decoration

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize

data class WindowDecorationState(
    val size: DpSize, val position: DpOffset, val isMinimized: Boolean, val isFullscreen: Boolean,
)