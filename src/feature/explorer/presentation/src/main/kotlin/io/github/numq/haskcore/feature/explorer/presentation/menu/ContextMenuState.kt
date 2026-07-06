package io.github.numq.haskcore.feature.explorer.presentation.menu

import io.github.numq.haskcore.feature.explorer.core.ExplorerNode

internal sealed interface ContextMenuState {
    data object Hidden : ContextMenuState

    data class Visible(val node: ExplorerNode, val x: Float, val y: Float) : ContextMenuState
}