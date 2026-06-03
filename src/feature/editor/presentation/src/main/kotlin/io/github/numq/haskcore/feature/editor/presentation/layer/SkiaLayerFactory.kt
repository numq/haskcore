package io.github.numq.haskcore.feature.editor.presentation.layer

import arrow.core.getOrElse
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.core.analysis.CodeIssue
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.core.syntax.Occurrence
import io.github.numq.haskcore.feature.editor.core.token.Token
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundLayer
import io.github.numq.haskcore.feature.editor.presentation.background.BackgroundOutlineLayer
import io.github.numq.haskcore.feature.editor.presentation.background.HighlightedLineLayer
import io.github.numq.haskcore.feature.editor.presentation.cache.PaintCache
import io.github.numq.haskcore.feature.editor.presentation.cache.ParagraphCache
import io.github.numq.haskcore.feature.editor.presentation.cache.TextLineCache
import io.github.numq.haskcore.feature.editor.presentation.caret.CaretLayer
import io.github.numq.haskcore.feature.editor.presentation.guideline.GuidelineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterActionLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterLineLayer
import io.github.numq.haskcore.feature.editor.presentation.gutter.GutterSeparatorLayer
import io.github.numq.haskcore.feature.editor.presentation.issue.IssueLayer
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.feature.editor.presentation.occurrence.OccurrenceLayer
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionLayer
import io.github.numq.haskcore.feature.editor.presentation.selection.SelectionRegionLayer
import io.github.numq.haskcore.feature.editor.presentation.text.TextContentLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportLine
import org.jetbrains.skia.Image
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect

internal class SkiaLayerFactory(
    private val textLineCache: TextLineCache,
    private val paintCache: PaintCache,
    private val paragraphCache: ParagraphCache,
) : LayerFactory {
    private fun buildToken(
        line: Int,
        start: Int,
        end: Int,
        type: Token.Type?,
        isAtom: Boolean,
        lineText: String,
    ): Token {
        val range = TextRange(
            start = TextPosition(line = line, column = start), end = TextPosition(line = line, column = end)
        )

        val finalType = type ?: Token.Type.UNKNOWN

        val extractedText = when {
            start < end && start in lineText.indices && end <= lineText.length -> {
                lineText.substring(start, end)
            }

            else -> ""
        }

        return when {
            isAtom || finalType != Token.Type.UNKNOWN -> Token.Atom(
                range = range, type = finalType, text = extractedText
            )

            else -> Token.Region(range = range, type = finalType)
        }
    }

    private fun mergeTokens(
        line: Int,
        semanticTokens: List<Token>,
        syntaxTokens: List<Token>,
        text: String,
    ) = when {
        text.isEmpty() -> emptyList()

        else -> {
            val finalTypes = arrayOfNulls<Token.Type>(text.length)

            val isAtomChar = BooleanArray(text.length)

            fun getPriority(type: Token.Type?) = when (type) {
                null, Token.Type.UNKNOWN -> 0

                Token.Type.VARIABLE -> 1

                else -> 2
            }

            fun writeTokenData(start: Int, end: Int, type: Token.Type, isAtom: Boolean) {
                val coercedStart = start.coerceIn(0, text.length)

                val coercedEnd = end.coerceIn(0, text.length)

                for (index in coercedStart until coercedEnd) {
                    val existingType = finalTypes[index]

                    when {
                        getPriority(type) >= getPriority(existingType) -> {
                            finalTypes[index] = type

                            isAtomChar[index] = isAtom
                        }
                    }
                }
            }

            val sortedSyntaxTokens = syntaxTokens.sortedByDescending { token ->
                token.range.end.column - token.range.start.column
            }

            sortedSyntaxTokens.forEach { syntaxToken ->
                writeTokenData(
                    start = syntaxToken.range.start.column,
                    end = syntaxToken.range.end.column,
                    type = syntaxToken.type,
                    isAtom = syntaxToken is Token.Atom
                )
            }

            semanticTokens.forEach { semanticToken ->
                writeTokenData(
                    start = semanticToken.range.start.column,
                    end = semanticToken.range.end.column,
                    type = semanticToken.type,
                    isAtom = semanticToken is Token.Atom
                )
            }

            val result = mutableListOf<Token>()

            var currentType = finalTypes[0]

            var currentIsAtom = isAtomChar[0]

            var startColumn = 0

            for (index in 1 until text.length) {
                val nextType = finalTypes[index]

                val nextIsAtom = isAtomChar[index]

                when {
                    nextType != currentType || nextIsAtom != currentIsAtom -> {
                        result.add(
                            buildToken(
                                line = line,
                                start = startColumn,
                                end = index,
                                type = currentType,
                                isAtom = currentIsAtom,
                                lineText = text
                            )
                        )

                        currentType = nextType

                        currentIsAtom = nextIsAtom

                        startColumn = index
                    }
                }
            }

            result.add(
                buildToken(
                    line = line,
                    start = startColumn,
                    end = text.length,
                    type = currentType,
                    isAtom = currentIsAtom,
                    lineText = text
                )
            )

            result
        }
    }

    override fun createBackgroundLayer(width: Float, height: Float, theme: EditorTheme) = BackgroundLayer(
        width = width, height = height, paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.backgroundColorPalette.backgroundColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createBackgroundOutlineLayer(width: Float, theme: EditorTheme) = BackgroundOutlineLayer(
        width = width, paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.backgroundColorPalette.backgroundOutlineColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createHighlightedLineLayer(
        viewportLines: List<ViewportLine>, caret: Caret, theme: EditorTheme,
    ): HighlightedLineLayer? {
        val viewportLine = viewportLines.firstOrNull { viewportLine ->
            viewportLine.line == caret.position.line
        } ?: return null

        return HighlightedLineLayer(
            x = viewportLine.x,
            y = viewportLine.y,
            width = viewportLine.width,
            height = viewportLine.height,
            paint = paintCache.getOrCreate(
                key = PaintCache.Key(color = theme.backgroundColorPalette.currentLineColor)
            ).getOrElse { throwable ->
                throw throwable
            })
    }

    override fun createGutterLineLayer(
        line: Int, width: Float, textY: Float, font: Font, theme: EditorTheme,
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

        val textX = width - font.lineHeight - Measurements.GUTTER_GAP - Measurements.GUTTER_PADDING_END - textLine.width

        return GutterLineLayer(
            line = line, text = text, textLine = textLine, paint = textPaint, textX = textX, textY = textY
        )
    }

    override fun createGutterActionLayers(
        viewportLines: List<ViewportLine>, image: Image, gutterWidth: Float, theme: EditorTheme,
    ) = viewportLines.map { line ->
        val actionSize = line.height * .9f

        val x = gutterWidth - actionSize - Measurements.GUTTER_PADDING_END

        val y = line.y + (line.height - actionSize) / 2

        GutterActionLayer(
            rect = Rect.makeXYWH(l = x, t = y, w = actionSize, h = actionSize), image = image
        )
    }

    override fun createGutterSeparatorLayer(x: Float, height: Float, theme: EditorTheme) = GutterSeparatorLayer(
        x = x, height = height, paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.gutterColorPalette.separatorColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createGuidelineLayer(
        column: Int, height: Float, scrollX: Float, font: Font, theme: EditorTheme,
    ) = GuidelineLayer(
        x = (column * font.charWidth) - scrollX + Measurements.EDITOR_PADDING_START,
        height = height,
        paint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.codeAreaColorPalette.guidelineColor)
        ).getOrElse { throwable ->
            throw throwable
        })

    override fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        semanticTokensPerLine: Map<Int, List<Token>>?,
        syntaxTokensPerLine: Map<Int, List<Token>>?,
        scrollX: Float,
        font: Font,
        theme: EditorTheme,
    ) = viewportLines.map { viewportLine ->
        val line = viewportLine.line

        val text = viewportLine.text

        val semanticTokens = semanticTokensPerLine?.get(line).orEmpty()

        val syntaxTokens = syntaxTokensPerLine?.get(line).orEmpty()

        val mergedTokens = mergeTokens(
            line = line, semanticTokens = semanticTokens, syntaxTokens = syntaxTokens, text = text
        )

        val paragraph = paragraphCache.getOrCreate(
            key = ParagraphCache.Key(
                text = text, tokens = mergedTokens, width = viewportLine.width, font = font, theme = theme
            )
        ).getOrElse { throwable ->
            throw throwable
        }

        TextContentLayer(
            viewportLine = viewportLine,
            paragraph = paragraph,
            x = -scrollX + Measurements.EDITOR_PADDING_START,
            y = viewportLine.textBaselineY + font.ascent
        )
    }

    override fun createOccurrenceLayers(
        contentLayers: List<TextContentLayer>,
        occurrences: List<Occurrence>,
        caret: Caret,
        scrollX: Float,
        theme: EditorTheme,
    ): List<OccurrenceLayer> {
        val usageHighlightPaint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.codeAreaColorPalette.usageHighlightBackground)
        ).getOrElse { throwable ->
            throw throwable
        }

        val currentUsageHighlightPaint = paintCache.getOrCreate(
            key = PaintCache.Key(color = theme.codeAreaColorPalette.currentUsageHighlightBackground)
        ).getOrElse { throwable ->
            throw throwable
        }

        return contentLayers.flatMap { contentLayer ->
            occurrences.filter { occurrence ->
                occurrence.range.start.line == contentLayer.viewportLine.line
            }.map { occurrenceToken ->
                val startX = contentLayer.getCoordinateAtOffset(occurrenceToken.range.start.column)

                val endX = contentLayer.getCoordinateAtOffset(occurrenceToken.range.end.column)

                val x = -scrollX + startX + Measurements.EDITOR_PADDING_START

                val paint = when {
                    occurrenceToken.range.contains(caret.position) -> currentUsageHighlightPaint

                    else -> usageHighlightPaint
                }

                OccurrenceLayer(
                    rect = Rect.makeXYWH(
                        l = x, t = contentLayer.viewportLine.y, w = endX - startX, h = contentLayer.viewportLine.height
                    ), paint = paint
                )
            }
        }
    }

    override fun createIssueLayers(
        contentLayers: List<TextContentLayer>, issues: List<CodeIssue>, scrollX: Float, theme: EditorTheme,
    ) = contentLayers.flatMap { layer ->
        val lineIssues = issues.filter { issue ->
            issue.range.start.line == layer.viewportLine.line
        }

        lineIssues.map { issue ->
            val startX = layer.getCoordinateAtOffset(issue.range.start.column)

            val endX = layer.getCoordinateAtOffset(issue.range.end.column)

            val color = when (issue) {
                is CodeIssue.Unknown -> theme.overlayColorPalette.unknownUnderlineColor

                is CodeIssue.Error -> theme.overlayColorPalette.errorUnderlineColor

                is CodeIssue.Warning -> theme.overlayColorPalette.warningUnderlineColor

                is CodeIssue.Information -> theme.overlayColorPalette.infoUnderlineColor

                is CodeIssue.Hint -> theme.overlayColorPalette.hintUnderlineColor
            }

            val paint = paintCache.getOrCreate(
                key = PaintCache.Key(color = color, mode = PaintMode.STROKE)
            ).getOrElse { throwable ->
                throw throwable
            }

            IssueLayer(
                startX = startX - scrollX + Measurements.EDITOR_PADDING_START,
                endX = endX - scrollX + Measurements.EDITOR_PADDING_START,
                baselineY = layer.viewportLine.textBaselineY,
                paint = paint
            )
        }
    }

    override fun createSelectionLayer(
        contentLayers: List<TextContentLayer>, selection: Selection, scrollX: Float, theme: EditorTheme,
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

    override fun createCaretLayer(
        contentLayers: List<TextContentLayer>, caret: Caret, scrollX: Float, font: Font, theme: EditorTheme,
    ): CaretLayer? {
        val contentLayer = contentLayers.firstOrNull { codeAreaContentLayer ->
            codeAreaContentLayer.viewportLine.line == caret.position.line
        } ?: return null

        val xOffset = contentLayer.getCoordinateAtOffset(offset = caret.position.column).takeIf { xOffset ->
            xOffset > 0f || caret.position.column == 0
        } ?: (caret.position.column * font.charWidth)

        return CaretLayer(
            x = -scrollX + xOffset + Measurements.EDITOR_PADDING_START,
            y = contentLayer.viewportLine.textBaselineY + font.ascent,
            height = font.textHeight,
            paint = paintCache.getOrCreate(
                key = PaintCache.Key(color = theme.codeAreaColorPalette.caretColor)
            ).getOrElse { throwable ->
                throw throwable
            })
    }
}