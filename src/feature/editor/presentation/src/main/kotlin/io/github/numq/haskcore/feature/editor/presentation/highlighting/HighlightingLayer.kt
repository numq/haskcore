package io.github.numq.haskcore.feature.editor.presentation.highlighting

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas

internal data class HighlightingLayer(val highlightingUsageLayers: List<HighlightingUsageLayer>) : Layer {
    override fun render(canvas: Canvas) {
        highlightingUsageLayers.forEach { highlightingUsageLayer ->
            highlightingUsageLayer.render(canvas = canvas)
        }
    }
}