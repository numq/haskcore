package io.github.numq.haskcore.platform.overlay.menu

import androidx.compose.ui.geometry.Offset

sealed interface MenuState {
    data object Hidden : MenuState

    data class Visible(val offset: Offset) : MenuState
}