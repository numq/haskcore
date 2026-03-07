package io.github.numq.haskcore.feature.editor.presentation.scrollbar

internal data class ScrollbarPosition(val horizontal: Float, val vertical: Float) {
    companion object {
        val ZERO = ScrollbarPosition(horizontal = 0f, vertical = 0f)
    }
}