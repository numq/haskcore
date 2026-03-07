package io.github.numq.haskcore.feature.editor.presentation.gutter

import io.github.numq.haskcore.feature.editor.presentation.layout.Layout
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

internal data class GutterLayout(
    override val viewport: Viewport,
    override val bounds: Rect,
    val separatorLayer: GutterSeparatorLayer,
    val lineLayers: List<GutterLineLayer> = emptyList(),
    val icons: List<GutterIcon> = emptyList()
) : Layout {
    override fun render(canvas: Canvas) {
        lineLayers.forEach { lineLayer ->
            lineLayer.render(canvas = canvas)
        }

        separatorLayer.render(canvas = canvas)
    }
}