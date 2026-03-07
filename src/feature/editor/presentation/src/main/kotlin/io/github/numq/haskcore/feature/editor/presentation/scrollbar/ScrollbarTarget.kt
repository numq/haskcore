package io.github.numq.haskcore.feature.editor.presentation.scrollbar

internal data class ScrollbarTarget(val horizontal: Float?, val vertical: Float?) {
    companion object {
        val EMPTY = ScrollbarTarget(horizontal = null, vertical = null)
    }
}