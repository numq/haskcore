package io.github.numq.haskcore.feature.editor.presentation.layer

import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.guideline.Guideline
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.core.syntax.Occurrence
import io.github.numq.haskcore.feature.editor.core.syntax.Token
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayer
import io.github.numq.haskcore.feature.editor.presentation.background.HighlightedLineLayer
import io.github.numq.haskcore.feature.editor.presentation.caret.CaretLayer
import io.github.numq.haskcore.feature.editor.presentation.guideline.GuidelineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterSeparatorLayer
import io.github.numq.haskcore.feature.editor.presentation.occurrence.OccurrenceLayer
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionLayer
import io.github.numq.haskcore.feature.editor.presentation.text.TextContentLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportLine
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme

interface LayerFactory {
    fun createBackgroundLayer(width: Float, height: Float, theme: EditorTheme): BackgroundLayer

    fun createHighlightedLineLayer(
        viewportLines: List<ViewportLine>, caret: Caret, theme: EditorTheme
    ): HighlightedLineLayer?

    fun createGutterLineLayer(
        line: Int, width: Float, textY: Float, font: EditorFont, theme: EditorTheme
    ): GutterLineLayer

    fun createGutterSeparatorLayer(x: Float, height: Float, theme: EditorTheme): GutterSeparatorLayer

    fun createGuidelineLayer(
        guideline: Guideline, height: Float, scrollX: Float, font: EditorFont, theme: EditorTheme
    ): GuidelineLayer

    fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        tokensPerLine: Map<Int, List<Token>>?,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): List<TextContentLayer>

    fun createOccurrenceLayers(
        contentLayers: List<TextContentLayer>,
        occurrences: List<Occurrence>,
        caret: Caret,
        scrollX: Float,
        theme: EditorTheme
    ): List<OccurrenceLayer>

    fun createSelectionLayer(
        contentLayers: List<TextContentLayer>, selection: Selection, scrollX: Float, theme: EditorTheme
    ): SelectionLayer

    fun createCaretLayer(
        contentLayers: List<TextContentLayer>, caret: Caret, scrollX: Float, font: EditorFont, theme: EditorTheme
    ): CaretLayer?
}