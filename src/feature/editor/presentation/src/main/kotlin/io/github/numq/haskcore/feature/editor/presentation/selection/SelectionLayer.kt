package io.github.numq.haskcore.feature.editor.presentation.selection

import io.github.numq.haskcore.feature.editor.presentation.layer.Layer
import org.jetbrains.skia.Canvas

internal data class SelectionLayer(val selectionRegionLayers: List<SelectionRegionLayer> = emptyList()) : Layer {
    override fun render(canvas: Canvas) {
        selectionRegionLayers.forEach { selectionRegionLayer ->
            selectionRegionLayer.render(canvas = canvas)
        }
    }
}