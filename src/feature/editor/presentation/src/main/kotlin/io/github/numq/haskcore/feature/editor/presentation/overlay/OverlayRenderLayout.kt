package io.github.numq.haskcore.feature.editor.presentation.overlay

import io.github.numq.haskcore.feature.editor.presentation.layout.Layout
import io.github.numq.haskcore.feature.editor.presentation.overlay.completion.AutocompleteBox
import io.github.numq.haskcore.feature.editor.presentation.overlay.drag.DragPreview
import io.github.numq.haskcore.feature.editor.presentation.overlay.error.ErrorMarker
import io.github.numq.haskcore.feature.editor.presentation.overlay.highlight.Highlight
import io.github.numq.haskcore.feature.editor.presentation.overlay.menu.ContextMenu
import io.github.numq.haskcore.feature.editor.presentation.overlay.tooltip.Tooltip
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

internal data class OverlayRenderLayout(
    override val viewport: Viewport,
    override val bounds: Rect,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val tooltips: List<Tooltip> = emptyList(),
    val errorMarkers: List<ErrorMarker> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val contextMenu: ContextMenu? = null,
    val autocomplete: AutocompleteBox? = null,
    val dragPreview: DragPreview? = null
) : Layout {
    override fun render(canvas: Canvas) {
        // todo
    }
}