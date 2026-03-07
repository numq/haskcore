package io.github.numq.haskcore.feature.editor.presentation.viewport

internal data class Viewport(
    val width: Float, val height: Float, val visibleLines: IntRange, val viewportLines: List<ViewportLine>
) {
    companion object {
        val EMPTY = Viewport(width = 0f, height = 0f, visibleLines = IntRange.EMPTY, viewportLines = emptyList())
    }
}