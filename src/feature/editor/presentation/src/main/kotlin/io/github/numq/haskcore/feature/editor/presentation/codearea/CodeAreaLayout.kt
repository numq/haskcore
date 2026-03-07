package io.github.numq.haskcore.feature.editor.presentation.codearea

import io.github.numq.haskcore.feature.editor.presentation.caret.CaretLayer
import io.github.numq.haskcore.feature.editor.presentation.guideline.GuidelineLayer
import io.github.numq.haskcore.feature.editor.presentation.highlighting.HighlightingLayer
import io.github.numq.haskcore.feature.editor.presentation.layout.Layout
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

internal data class CodeAreaLayout(
    override val viewport: Viewport,
    override val bounds: Rect,
    val contentLayers: List<CodeAreaContentLayer> = emptyList(),
    val guidelineLayer: GuidelineLayer? = null,
    val highlightingLayer: HighlightingLayer? = null,
    val selectionLayer: SelectionLayer? = null,
    val caretLayer: CaretLayer? = null,
) : Layout {
    override fun render(canvas: Canvas) {
        guidelineLayer?.render(canvas = canvas)

        selectionLayer?.render(canvas = canvas) ?: highlightingLayer?.render(canvas = canvas)

        contentLayers.forEach { contentLayer ->
            contentLayer.render(canvas = canvas)
        }

        caretLayer?.render(canvas = canvas)
    }
}