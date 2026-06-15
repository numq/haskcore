package io.github.numq.haskcore.common.presentation.overlay.menu

sealed interface ContextMenuState {
    data object Hidden : ContextMenuState

    data class Visible(val x: Float, val y: Float) : ContextMenuState
}