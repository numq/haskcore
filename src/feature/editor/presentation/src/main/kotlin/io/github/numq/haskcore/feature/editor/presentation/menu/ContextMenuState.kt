package io.github.numq.haskcore.feature.editor.presentation.menu

internal sealed interface ContextMenuState {
    data object Hidden : ContextMenuState

    data class Visible(val x: Float, val y: Float) : ContextMenuState
}