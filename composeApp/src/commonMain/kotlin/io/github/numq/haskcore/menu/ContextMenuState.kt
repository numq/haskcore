package io.github.numq.haskcore.menu

import androidx.compose.ui.geometry.Offset

internal sealed interface ContextMenuState {
    data object Hidden : ContextMenuState

    data class Visible<T>(val offset: Offset, val payload: T) : ContextMenuState
}