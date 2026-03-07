package io.github.numq.haskcore.feature.editor.presentation.layer

import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayer
import io.github.numq.haskcore.feature.editor.presentation.background.CurrentLineLayer
import io.github.numq.haskcore.feature.editor.presentation.caret.CaretLayer
import io.github.numq.haskcore.feature.editor.presentation.codearea.CodeAreaContentLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterSeparatorLayer
import io.github.numq.haskcore.feature.editor.presentation.highlighting.HighlightingLayer
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportLine
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.jetbrains.skia.Rect

internal interface LayerFactory {
    fun createBackgroundLayer(bounds: Rect, theme: EditorTheme): BackgroundLayer

    fun createCurrentLineLayer(viewport: Viewport, caret: Caret, theme: EditorTheme): CurrentLineLayer?

    fun createGutterLineLayer(
        line: Int, width: Float, textY: Float, font: EditorFont, theme: EditorTheme
    ): GutterLineLayer

    fun createGutterSeparatorLayer(x: Float, y: Float, theme: EditorTheme): GutterSeparatorLayer

    fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        highlighting: Highlighting,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): List<CodeAreaContentLayer>

    fun createHighlightingLayer(
        caret: Caret,
        highlighting: Highlighting,
        contentLayers: List<CodeAreaContentLayer>,
        scrollX: Float,
        theme: EditorTheme
    ): HighlightingLayer

    fun createCaretLayer(
        caret: Caret, contentLayers: List<CodeAreaContentLayer>, scrollX: Float, font: EditorFont, theme: EditorTheme
    ): CaretLayer?

    fun createSelectionLayer(
        selection: Selection, contentLayers: List<CodeAreaContentLayer>, scrollX: Float, theme: EditorTheme
    ): SelectionLayer
}