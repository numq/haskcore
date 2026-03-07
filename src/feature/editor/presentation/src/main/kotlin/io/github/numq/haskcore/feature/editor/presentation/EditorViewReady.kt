package io.github.numq.haskcore.feature.presentation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.*
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.highlighting.Highlighting
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.presentation.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.codearea.CodeAreaLayout
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.layout.LayoutFactory
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.ScrollbarTarget
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportCalculator
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Rect
import java.awt.Cursor
import kotlin.math.max

@Composable
internal fun EditorViewReady(
    snapshot: TextSnapshot,
    caret: Caret,
    selection: Selection,
    highlighting: Highlighting,
    layerFactory: LayerFactory,
    layoutFactory: LayoutFactory,
    font: EditorFont,
    theme: EditorTheme,
    execute: suspend (EditorCommand) -> Unit
) {
    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()

    val verticalScrollState = rememberScrollState()

    val gutterWidth by remember(snapshot.lines) {
        derivedStateOf {
            val maxTextWidth = font.measureTextWidth("${snapshot.lines + 1}")

            Measurements.GUTTER_PADDING_START + maxTextWidth + Measurements.GUTTER_GAP + Measurements.GUTTER_ACTION_WIDTH + Measurements.GUTTER_PADDING_END
        }
    }

    val contentWidth by remember(snapshot.maxLineLength, font.charWidth, gutterWidth) {
        derivedStateOf {
            val textWidth = snapshot.maxLineLength * font.charWidth

            gutterWidth + Measurements.EDITOR_PADDING_START + textWidth + Measurements.EDITOR_PADDING_END
        }
    }

    val contentHeight by remember(snapshot.lines, font.lineHeight) {
        derivedStateOf {
            snapshot.lines * font.lineHeight + Measurements.CONTENT_PADDING_BOTTOM
        }
    }

    val selectionCommands = remember {
        Channel<EditorCommand.TextSelection>(Channel.CONFLATED)
    }

    LaunchedEffect(selectionCommands) {
        for (command in selectionCommands) {
            execute(command)
        }
    }

    DisposableEffect(selectionCommands) {
        onDispose {
            selectionCommands.close()
        }
    }

    Scrollbar(
        modifier = Modifier.fillMaxSize(),
        horizontalScrollState = horizontalScrollState,
        verticalScrollState = verticalScrollState,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        paddingStart = gutterWidth,
        minimalHeight = Measurements.SCROLLBAR_MINIMAL_SIZE,
        thickness = Measurements.SCROLLBAR_THICKNESS,
        theme = theme,
        content = {
            val viewportWidth = constraints.maxWidth.toFloat()

            val viewportHeight = constraints.maxHeight.toFloat()

            val scrollX = horizontalScrollState.value

            val scrollY = verticalScrollState.value

            val caretVisible by produceState(true, isFocused) {
                if (!isFocused) {
                    value = false

                    return@produceState
                }

                while (isActive) {
                    value = true

                    delay(530L)

                    value = false

                    delay(530L)
                }
            }

            val viewport by remember(snapshot, viewportWidth, viewportHeight, scrollY, font) {
                derivedStateOf {
                    ViewportCalculator.calculate(
                        snapshot = snapshot,
                        width = viewportWidth,
                        height = viewportHeight,
                        scrollY = scrollY.toFloat(),
                        ascent = font.ascent,
                        textHeight = font.textHeight,
                        lineHeight = font.lineHeight,
                    )
                }
            }

            val backgroundLayout by remember(viewport, theme) {
                derivedStateOf {
                    layoutFactory.createBackgroundLayout(viewport = viewport, theme = theme)
                }
            }

            val backgroundLayoutWithHighlighting by remember(backgroundLayout, viewport, caret, theme) {
                derivedStateOf {
                    backgroundLayout.copy(
                        currentLineLayer = layerFactory.createCurrentLineLayer(
                            viewport = viewport, caret = caret, theme = theme
                        )
                    )
                }
            }

            val gutterLayout by remember(viewport, gutterWidth, font, theme, snapshot.revision) {
                derivedStateOf {
                    layoutFactory.createGutterLayout(
                        viewport = viewport, width = gutterWidth, font = font, theme = theme
                    )
                }
            }

            val codeAreaLayout by remember(
                viewport, highlighting, gutterWidth, scrollX, font, theme, snapshot.revision
            ) {
                derivedStateOf {
                    layoutFactory.createCodeAreaLayout(
                        viewport = viewport,
                        highlighting = highlighting,
                        gutterWidth = gutterWidth,
                        scrollX = scrollX.toFloat(),
                        font = font,
                        theme = theme
                    )
                }
            }

            LaunchedEffect(codeAreaLayout) {
                val contentLayers = codeAreaLayout.contentLayers

                if (contentLayers.isNotEmpty()) {
                    val startLine = contentLayers.firstOrNull()?.viewportLine?.line ?: 0

                    val endLine = contentLayers.lastOrNull()?.viewportLine?.line ?: 0

                    execute(EditorCommand.RequestHighlightingUpdate(startLine = startLine, endLine = endLine))
                }
            }

            val codeAreaLayoutWithHighlighting by remember(codeAreaLayout, highlighting, scrollX, font, theme) {
                derivedStateOf {
                    val highlightingLayer = layerFactory.createHighlightingLayer(
                        caret = caret,
                        highlighting = highlighting,
                        contentLayers = codeAreaLayout.contentLayers,
                        scrollX = scrollX.toFloat(),
                        theme = theme
                    )

                    codeAreaLayout.copy(highlightingLayer = highlightingLayer)
                }
            }

            val codeAreaLayoutWithSelection by remember(
                codeAreaLayoutWithHighlighting, selection, scrollX, font, theme
            ) {
                derivedStateOf {
                    val selectionLayer = layerFactory.createSelectionLayer(
                        selection = selection,
                        contentLayers = codeAreaLayoutWithHighlighting.contentLayers,
                        scrollX = scrollX.toFloat(),
                        theme = theme
                    )

                    codeAreaLayoutWithHighlighting.copy(selectionLayer = selectionLayer)
                }
            }

            val codeAreaLayoutWithCaret by remember(
                codeAreaLayoutWithSelection, caret, scrollX, font, theme, caretVisible
            ) {
                derivedStateOf {
                    when {
                        caretVisible -> {
                            val caretLayer = layerFactory.createCaretLayer(
                                caret = caret,
                                contentLayers = codeAreaLayoutWithSelection.contentLayers,
                                scrollX = scrollX.toFloat(),
                                font = font,
                                theme = theme
                            )

                            codeAreaLayoutWithSelection.copy(caretLayer = caretLayer)
                        }

                        else -> codeAreaLayoutWithSelection
                    }
                }
            }

            val scrollbarTarget by remember(
                caret, font, viewportWidth, viewportHeight, scrollX, scrollY, gutterWidth
            ) {
                derivedStateOf {
                    val (line, column) = caret.position

                    val caretTextOffset = column * font.charWidth

                    val absoluteLineTop = line * font.lineHeight

                    val absoluteLineBottom = absoluteLineTop + font.lineHeight

                    val absoluteCaretX = Measurements.EDITOR_PADDING_START + caretTextOffset

                    val verticalMargin = font.lineHeight

                    val horizontalMargin = font.charWidth * 2

                    val editorVisibleWidth = viewportWidth - gutterWidth

                    val newHorizontal = when {
                        absoluteCaretX < scrollX + horizontalMargin -> max(
                            0f, absoluteCaretX - horizontalMargin
                        )

                        absoluteCaretX > scrollX + editorVisibleWidth - horizontalMargin -> absoluteCaretX - editorVisibleWidth + horizontalMargin

                        else -> null
                    }

                    val newVertical = when {
                        absoluteLineTop < scrollY + verticalMargin -> max(
                            0f, absoluteLineTop - verticalMargin
                        )

                        absoluteLineBottom > scrollY + viewportHeight - verticalMargin -> absoluteLineBottom - viewportHeight + verticalMargin

                        else -> null
                    }

                    ScrollbarTarget(horizontal = newHorizontal, vertical = newVertical)
                }
            }

            LaunchedEffect(caret) {
                scrollbarTarget.horizontal?.toInt()?.let { value -> horizontalScrollState.scrollTo(value) }

                scrollbarTarget.vertical?.toInt()?.let { value -> verticalScrollState.scrollTo(value) }
            }

            var draggingMousePosition by remember { mutableStateOf<Offset?>(null) }

            LaunchedEffect(draggingMousePosition) {
                val position = draggingMousePosition

                if (position != null) {
                    while (isActive) {
                        val currentY = position.y

                        val scrollThreshold = 60f

                        val scrollSpeed = 20f

                        val delta = when {
                            currentY > viewportHeight - scrollThreshold -> scrollSpeed * ((currentY - (viewportHeight - scrollThreshold)) / scrollThreshold).coerceAtMost(
                                1f
                            )

                            currentY < scrollThreshold -> -scrollSpeed * ((scrollThreshold - currentY) / scrollThreshold).coerceAtMost(
                                1f
                            )

                            else -> 0f
                        }

                        if (delta != 0f) {
                            verticalScrollState.scrollBy(delta)

                            val newPosition = calculatePositionAtOffset(
                                offset = position,
                                viewport = viewport,
                                codeAreaLayout = codeAreaLayout,
                                gutterWidth = gutterWidth,
                                scrollX = scrollX.toFloat(),
                                snapshot = snapshot
                            )

                            selectionCommands.trySend(EditorCommand.TextSelection.Extend(position = newPosition))
                        }

                        delay(10)
                    }
                }
            }

            val currentViewport by rememberUpdatedState(viewport)

            val currentCodeAreaLayout by rememberUpdatedState(codeAreaLayout)

            Box(
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester).onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }.focusable().onKeyEvent { keyEvent ->
                    when (keyEvent.type) {
                        KeyEventType.KeyDown -> {
                            keyEvent.awtEventOrNull?.let { awtEvent ->
                                scope.launch {
                                    execute(
                                        EditorCommand.ProcessKey(
                                            keyCode = awtEvent.keyCode,
                                            modifiers = awtEvent.modifiersEx,
                                            utf16CodePoint = keyEvent.utf16CodePoint
                                        )
                                    )
                                }
                            }

                            true
                        }

                        else -> false
                    }
                }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            focusRequester.requestFocus()

                            val position = calculatePositionAtOffset(
                                offset = down.position,
                                viewport = currentViewport,
                                codeAreaLayout = currentCodeAreaLayout,
                                gutterWidth = gutterWidth,
                                scrollX = scrollX.toFloat(),
                                snapshot = snapshot
                            )

                            scope.launch {
                                execute(
                                    EditorCommand.MoveCaret(position = position)
                                )
                            }

                            selectionCommands.trySend(EditorCommand.TextSelection.Start(position = position))

                            draggingMousePosition = down.position

                            var dragEvent = down

                            while (true) {
                                val event = awaitPointerEvent()

                                val anyPressed = event.changes.any(PointerInputChange::pressed)

                                if (!anyPressed) {
                                    break
                                }

                                val currentDrag = event.changes.firstOrNull()

                                if (currentDrag != null && currentDrag.position != dragEvent.position) {
                                    dragEvent = currentDrag

                                    draggingMousePosition = currentDrag.position

                                    val currentPosition = calculatePositionAtOffset(
                                        offset = currentDrag.position,
                                        viewport = currentViewport,
                                        codeAreaLayout = currentCodeAreaLayout,
                                        gutterWidth = gutterWidth,
                                        scrollX = scrollX.toFloat(),
                                        snapshot = snapshot
                                    )

                                    selectionCommands.trySend(EditorCommand.TextSelection.Extend(position = currentPosition))

                                    if (currentDrag.positionChange() != Offset.Zero) {
                                        currentDrag.consume()
                                    }
                                }
                            }
                        }
                    }
                }.drawWithCache {
                    val bounds = Rect.makeWH(w = size.width, h = size.height)

                    onDrawBehind {
                        if (!bounds.isEmpty) {
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas

                                nativeCanvas.save()

                                nativeCanvas.clipRect(r = Rect.makeWH(w = bounds.width, h = bounds.height))

                                nativeCanvas.clear(color = theme.backgroundColorPalette.backgroundColor)

                                backgroundLayoutWithHighlighting.render(canvas = nativeCanvas)

                                gutterLayout.render(canvas = nativeCanvas)

                                nativeCanvas.save()

                                nativeCanvas.translate(dx = gutterWidth, dy = 0f)

                                nativeCanvas.clipRect(
                                    r = Rect.makeWH(w = bounds.width - gutterWidth, h = bounds.height)
                                )

                                codeAreaLayoutWithCaret.render(canvas = nativeCanvas)

                                nativeCanvas.restore()

                                nativeCanvas.restore()
                            }
                        }
                    }
                }.pointerHoverIcon(PointerIcon(Cursor(Cursor.TEXT_CURSOR)))
            )
        })
}

private fun calculatePositionAtOffset(
    offset: Offset,
    viewport: Viewport,
    codeAreaLayout: CodeAreaLayout,
    gutterWidth: Float,
    scrollX: Float,
    snapshot: TextSnapshot
): TextPosition {
    val clickedViewportLine = viewport.viewportLines.find { viewportLine ->
        offset.y >= viewportLine.y && offset.y <= (viewportLine.y + viewportLine.height)
    }

    return when (clickedViewportLine) {
        null -> {
            val lastLine = (snapshot.lines - 1).coerceAtLeast(0)

            val lineLength = snapshot.getLineLength(line = lastLine)

            TextPosition(line = lastLine, column = lineLength)
        }

        else -> {
            val targetLineIndex = clickedViewportLine.line

            val clickedLayer = codeAreaLayout.contentLayers.find { contentLayer ->
                contentLayer.viewportLine.line == targetLineIndex
            }

            val column = clickedLayer?.getOffsetAtCoordinate(
                targetX = max(0f, offset.x - gutterWidth + scrollX)
            ) ?: snapshot.getLineLength(line = targetLineIndex)

            TextPosition(line = targetLineIndex, column = column)
        }
    }
}