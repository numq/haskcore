package io.github.numq.haskcore.feature.output.presentation.menu

sealed interface OutputMenu {
    data object Hidden : OutputMenu

    data class Visible(val x: Float, val y: Float) : OutputMenu
}