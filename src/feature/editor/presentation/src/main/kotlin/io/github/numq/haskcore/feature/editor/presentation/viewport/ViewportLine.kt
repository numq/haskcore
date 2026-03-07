package io.github.numq.haskcore.feature.editor.presentation.viewport

internal data class ViewportLine(
    val line: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String,
    val textBaselineY: Float
)