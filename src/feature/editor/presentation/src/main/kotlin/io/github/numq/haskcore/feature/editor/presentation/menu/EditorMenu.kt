package io.github.numq.haskcore.feature.editor.presentation.menu

internal sealed interface EditorMenu {
    data object Hidden : EditorMenu

    data class Visible(val x: Float, val y: Float) : EditorMenu
}