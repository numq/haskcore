package io.github.numq.haskcore.feature.editor.presentation.layer

import arrow.core.getOrElse
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayer
import io.github.numq.haskcore.feature.editor.presentation.background.CurrentLineLayer
import io.github.numq.haskcore.feature.editor.presentation.cache.PaintCache
import io.github.numq.haskcore.feature.editor.presentation.cache.ParagraphCache
import io.github.numq.haskcore.feature.editor.presentation.cache.TextLineCache
import io.github.numq.haskcore.feature.editor.presentation.caret.CaretLayer
import io.github.numq.haskcore.feature.editor.presentation.codearea.CodeAreaContentLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterSeparatorLayer
import io.github.numq.haskcore.feature.editor.presentation.highlighting.HighlightingLayer
import io.github.numq.haskcore.feature.editor.presentation.highlighting.HighlightingUsageLayer
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionLayer
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionRegionLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportLine
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import org.jetbrains.skia.Rect

internal class SkiaLayerFactory(
    private val textLineCache: TextLineCache,
    private val paintCache: PaintCache,
    private val paragraphCache: ParagraphCache,
) : LayerFactory {
    override fun createBackgroundLayer(bounds: Rect, theme: EditorTheme) = BackgroundLayer(
        bounds = bounds, paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.backgroundColorPalette.backgroundColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createCurrentLineLayer(viewport: Viewport, caret: Caret, theme: EditorTheme): CurrentLineLayer? {
        val viewportLine = viewport.viewportLines.firstOrNull { viewportLine ->
            viewportLine.line == caret.position.line
        } ?: return null

        val bounds = with(viewportLine) {
            Rect.makeXYWH(l = x, t = y, w = width, h = height)
        }

        return CurrentLineLayer(
            bounds = bounds, paint = paintCache.getOrCreate(
                key = PaintCache.Key(color = theme.backgroundColorPalette.currentLineColor)
            ).getOrElse { throwable ->
                throw throwable
            })
    }

    override fun createGutterLineLayer(
        line: Int, width: Float, textY: Float, font: EditorFont, theme: EditorTheme
    ): GutterLineLayer {
        val text = "${line + 1}"

        val textLine = textLineCache.getOrCreate(
            key = TextLineCache.Key(text = text, font = font)
        ).getOrElse { throwable ->
            throw throwable
        }

        val textPaint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.gutterColorPalette.textColor)
        ).getOrElse { throwable ->
            throw throwable
        }

        val textX =
            width - Measurements.GUTTER_ACTION_WIDTH - Measurements.GUTTER_GAP - Measurements.GUTTER_PADDING_END - textLine.width

        return GutterLineLayer(
            line = line, text = text, textLine = textLine, paint = textPaint, textX = textX, textY = textY
        )
    }

    override fun createGutterSeparatorLayer(x: Float, y: Float, theme: EditorTheme) = GutterSeparatorLayer(
        x = x, y = y, paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.gutterColorPalette.textColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        highlighting: Highlighting,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ) = viewportLines.map { viewportLine ->
        val line = viewportLine.line

        val text = viewportLine.text

        val tokens = highlighting.tokensPerLine[line] ?: emptyList()

        val paragraph = paragraphCache.getOrCreate(
            key = ParagraphCache.Key(
                text = text, tokens = tokens, width = viewportLine.width, font = font, theme = theme
            )
        ).getOrElse { throwable ->
            throw throwable
        }

        CodeAreaContentLayer(
            viewportLine = viewportLine,
            paragraph = paragraph,
            x = -scrollX + Measurements.EDITOR_PADDING_START,
            y = viewportLine.textBaselineY + font.ascent
        )
    }

    override fun createHighlightingLayer(
        caret: Caret,
        highlighting: Highlighting,
        contentLayers: List<CodeAreaContentLayer>,
        scrollX: Float,
        theme: EditorTheme
    ): HighlightingLayer {
        val usagePaint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.codeAreaColorPalette.usageHighlightBackground)
        ).getOrElse { throwable ->
            throw throwable
        }

        val currentPaint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.codeAreaColorPalette.currentUsageHighlightBackground)
        ).getOrElse { throwable ->
            throw throwable
        }

        val highlightingUsageLayers = contentLayers.flatMap { contentLayer ->
            val line = contentLayer.viewportLine.line

            val highlightingTokens = highlighting.tokensPerLine[line] ?: emptyList()

            highlightingTokens.map { highlightingToken ->
                val startX = contentLayer.getCoordinateAtOffset(highlightingToken.range.start.column)

                val endX = contentLayer.getCoordinateAtOffset(highlightingToken.range.end.column)

                val x = -scrollX + startX + Measurements.EDITOR_PADDING_START

                val paint = when {
                    highlightingToken.range.contains(caret.position) -> currentPaint

                    else -> usagePaint
                }

                HighlightingUsageLayer(
                    rect = Rect.makeXYWH(
                        l = x, t = contentLayer.viewportLine.y, w = endX - startX, h = contentLayer.viewportLine.height
                    ), paint = paint
                )
            }
        }

        return HighlightingLayer(highlightingUsageLayers = highlightingUsageLayers)
    }

    override fun createCaretLayer(
        caret: Caret, contentLayers: List<CodeAreaContentLayer>, scrollX: Float, font: EditorFont, theme: EditorTheme
    ): CaretLayer? {
        val contentLayer = contentLayers.firstOrNull { codeAreaContentLayer ->
            codeAreaContentLayer.viewportLine.line == caret.position.line
        } ?: return null

        val xOffset = contentLayer.getCoordinateAtOffset(offset = caret.position.column).takeIf { xOffset ->
            xOffset > 0f || caret.position.column == 0
        } ?: (caret.position.column * font.charWidth)

        val bounds = Rect.makeXYWH(
            l = -scrollX + xOffset + Measurements.EDITOR_PADDING_START,
            t = contentLayer.viewportLine.textBaselineY + font.ascent,
            w = Measurements.CARET_WIDTH,
            h = font.textHeight
        )

        return CaretLayer(
            bounds = bounds, paint = paintCache.getOrCreate(
                key = PaintCache.Key(color = theme.codeAreaColorPalette.caretColor)
            ).getOrElse { throwable ->
                throw throwable
            })
    }

    override fun createSelectionLayer(
        selection: Selection, contentLayers: List<CodeAreaContentLayer>, scrollX: Float, theme: EditorTheme
    ) = when {
        selection.range.isEmpty -> SelectionLayer()

        else -> {
            val selectionPaint = paintCache.getOrCreate(
                key = PaintCache.Key(color = theme.codeAreaColorPalette.selectionColor)
            ).getOrElse { throwable ->
                throw throwable
            }

            val (start, end) = selection.range

            val selectionRegionLayers = mutableListOf<SelectionRegionLayer>()

            contentLayers.filter { contentLayer ->
                contentLayer.viewportLine.line in start.line..end.line
            }.forEach { layer ->
                val line = layer.viewportLine.line

                val lineLength = layer.viewportLine.text.length

                val relStartX = when (line) {
                    start.line -> layer.getCoordinateAtOffset(start.column.coerceIn(0, lineLength))

                    else -> 0f
                }

                val relEndX = when (line) {
                    end.line -> layer.getCoordinateAtOffset(end.column.coerceIn(0, lineLength))

                    else -> layer.viewportLine.width + scrollX
                }

                val baseOffset = -scrollX + Measurements.EDITOR_PADDING_START

                val left = (baseOffset + relStartX).coerceIn(0f, layer.viewportLine.width)

                val right = (baseOffset + relEndX).coerceIn(0f, layer.viewportLine.width)

                if (left < right) {
                    selectionRegionLayers.add(
                        SelectionRegionLayer(
                            left = left,
                            top = layer.viewportLine.y,
                            right = right,
                            bottom = layer.viewportLine.y + layer.viewportLine.height,
                            paint = selectionPaint
                        )
                    )
                }
            }

            SelectionLayer(selectionRegionLayers = selectionRegionLayers)
        }
    }
}