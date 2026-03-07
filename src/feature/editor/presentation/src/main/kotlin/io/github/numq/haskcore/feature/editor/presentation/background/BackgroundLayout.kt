package io.github.numq.haskcore.feature.editor.presentation.background

import io.github.numq.haskcore.feature.editor.presentation.layout.Layout
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

internal data class BackgroundLayout(
    override val viewport: Viewport,
    override val bounds: Rect,
    val backgroundLayer: BackgroundLayer,
    val currentLineLayer: CurrentLineLayer? = null
) : Layout {
    override fun render(canvas: Canvas) {
        backgroundLayer.render(canvas = canvas)

        currentLineLayer?.render(canvas = canvas)
    }
}