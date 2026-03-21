package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.feature.editor.presentation.mouse.EditorMouseHandler
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.ScrollbarContainer
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportCalculator
import io.github.numq.haskcore.platform.font.EditorFont
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Rect
import java.awt.Cursor
import kotlin.math.absoluteValue

@Composable
internal fun EditorViewReady(
    state: EditorState.Ready,
    font: EditorFont,
    theme: EditorTheme,
    layerFactory: LayerFactory,
    execute: suspend (EditorCommand) -> Unit
) {
    val scope = rememberCoroutineScope()

    val focusRequester = remember {
        FocusRequester()
    }

    var isFocused by remember {
        mutableStateOf(false)
    }

    val gutterWidth by remember(state.editor.snapshot.lines) {
        derivedStateOf {
            val maxTextWidth = font.measureTextWidth("${state.editor.snapshot.lines + 1}")

            Measurements.GUTTER_PADDING_START + maxTextWidth + Measurements.GUTTER_GAP + Measurements.GUTTER_ACTION_WIDTH + Measurements.GUTTER_PADDING_END
        }
    }

    val contentWidth by remember(state.editor.snapshot.maxLineLength, font.charWidth, gutterWidth) {
        derivedStateOf {
            val textWidth = state.editor.snapshot.maxLineLength * font.charWidth

            gutterWidth + Measurements.EDITOR_PADDING_START + textWidth + Measurements.EDITOR_PADDING_END
        }
    }

    val contentHeight by remember(state.editor.snapshot.lines, font.lineHeight) {
        derivedStateOf {
            state.editor.snapshot.lines * font.lineHeight + Measurements.CONTENT_PADDING_BOTTOM
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

    ScrollbarContainer(
        modifier = Modifier.fillMaxSize(),
        scrollbar = state.scrollbar,
        scroll = { x, y, viewportWidth, viewportHeight ->
            scope.launch {
                execute(
                    EditorCommand.Scroll(
                        x = x,
                        y = y,
                        contentWidth = contentWidth,
                        contentHeight = contentHeight,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight
                    )
                )
            }
        },
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        paddingStart = gutterWidth,
        minimalHeight = Measurements.SCROLLBAR_MINIMAL_SIZE,
        thickness = Measurements.SCROLLBAR_THICKNESS,
        theme = theme,
        content = { viewportWidth, viewportHeight ->
            var caretVisible by remember { mutableStateOf(true) }

            LaunchedEffect(isFocused, state.editor.caret.position) {
                if (!isFocused) {
                    caretVisible = false

                    return@LaunchedEffect
                }

                caretVisible = true

                while (isActive) {
                    delay(530L)

                    caretVisible = !caretVisible
                }
            }

            val viewport by remember(state.editor.snapshot, viewportWidth, viewportHeight, state.scrollbar.y, font) {
                derivedStateOf {
                    ViewportCalculator.calculate(
                        snapshot = state.editor.snapshot,
                        width = viewportWidth,
                        height = viewportHeight,
                        scrollY = state.scrollbar.y,
                        ascent = font.ascent,
                        textHeight = font.textHeight,
                        lineHeight = font.lineHeight,
                    )
                }
            }

            val currentViewport by rememberUpdatedState(viewport)

            val backgroundLayer by remember(currentViewport.width, currentViewport.height, theme) {
                derivedStateOf {
                    layerFactory.createBackgroundLayer(
                        width = currentViewport.width, height = currentViewport.height, theme = theme
                    )
                }
            }

            val currentBackgroundLayer by rememberUpdatedState(backgroundLayer)

            val highlightedLineLayer by remember(currentViewport.viewportLines, state.editor.caret, theme) {
                derivedStateOf {
                    layerFactory.createHighlightedLineLayer(
                        viewportLines = currentViewport.viewportLines, caret = state.editor.caret, theme = theme
                    )
                }
            }

            val currentHighlightedLineLayer by rememberUpdatedState(highlightedLineLayer)

            val gutterLineLayers by remember(currentViewport.viewportLines, gutterWidth, font, theme) {
                derivedStateOf {
                    currentViewport.viewportLines.map { viewportLine ->
                        layerFactory.createGutterLineLayer(
                            line = viewportLine.line,
                            width = gutterWidth,
                            textY = viewportLine.textBaselineY,
                            font = font,
                            theme = theme
                        )
                    }
                }
            }

            val currentGutterLineLayers by rememberUpdatedState(gutterLineLayers)

            val gutterSeparatorLayer by remember(gutterWidth, currentViewport.height, theme) {
                derivedStateOf {
                    layerFactory.createGutterSeparatorLayer(
                        x = gutterWidth, height = currentViewport.height, theme = theme
                    )
                }
            }

            val currentGutterSeparatorLayer by rememberUpdatedState(gutterSeparatorLayer)

            val guidelineLayer by remember(
                state.editor.guideline, currentViewport.height, state.scrollbar.x, font, theme
            ) {
                derivedStateOf {
                    state.editor.guideline?.let { guideline ->
                        layerFactory.createGuidelineLayer(
                            guideline = guideline,
                            height = currentViewport.height,
                            scrollX = state.scrollbar.x,
                            font = font,
                            theme = theme
                        )
                    }
                }
            }

            val currentGuidelineLayer by rememberUpdatedState(guidelineLayer)

            val contentLayers by remember(
                currentViewport.viewportLines, state.editor.syntax, state.scrollbar.x, font, theme
            ) {
                derivedStateOf {
                    layerFactory.createCodeAreaContentLayers(
                        viewportLines = currentViewport.viewportLines,
                        tokensPerLine = state.editor.syntax?.tokensPerLine,
                        scrollX = state.scrollbar.x,
                        font = font,
                        theme = theme
                    )
                }
            }

            val currentContentLayers by rememberUpdatedState(contentLayers)

            val selectionLayer by remember(state.editor.selection, currentContentLayers, state.scrollbar.x, theme) {
                derivedStateOf {
                    layerFactory.createSelectionLayer(
                        selection = state.editor.selection,
                        contentLayers = currentContentLayers,
                        scrollX = state.scrollbar.x,
                        theme = theme
                    )
                }
            }

            val currentSelectionLayer by rememberUpdatedState(selectionLayer)

            val occurrenceLayers by remember(
                state.editor.caret, state.editor.syntax, currentContentLayers, state.scrollbar.x, theme
            ) {
                derivedStateOf {
                    when (val highlighting = state.editor.syntax) {
                        null -> emptyList()

                        else -> layerFactory.createOccurrenceLayers(
                            caret = state.editor.caret,
                            occurrences = highlighting.occurrences,
                            contentLayers = currentContentLayers,
                            scrollX = state.scrollbar.x,
                            theme = theme
                        )
                    }
                }
            }

            val currentOccurrenceLayers by rememberUpdatedState(occurrenceLayers)

            val caretLayer by remember(state.editor.caret, currentContentLayers, state.scrollbar.x, font, theme) {
                derivedStateOf {
                    layerFactory.createCaretLayer(
                        caret = state.editor.caret,
                        contentLayers = currentContentLayers,
                        scrollX = state.scrollbar.x,
                        font = font,
                        theme = theme
                    )
                }
            }

            val currentCaretLayer by rememberUpdatedState(caretLayer)

            LaunchedEffect(currentContentLayers) {
                if (currentContentLayers.isNotEmpty()) {
                    val start = currentContentLayers.firstOrNull()?.viewportLine?.line ?: 0

                    val end = currentContentLayers.lastOrNull()?.viewportLine?.line ?: 0

                    execute(EditorCommand.UpdateViewport(start = start, end = end))
                }
            }

            LaunchedEffect(
                state.editor.caret.position, font, currentViewport.width, currentViewport.height, gutterWidth
            ) {
                val newScrollbar = state.scrollbar.calculateScrollOffset(
                    position = state.editor.caret.position,
                    font = font,
                    viewportWidth = currentViewport.width,
                    viewportHeight = currentViewport.height,
                    gutterWidth = gutterWidth
                )

                if ((newScrollbar.x - state.scrollbar.x).absoluteValue > 1f || (newScrollbar.y - state.scrollbar.y).absoluteValue > 1f) {
                    execute(
                        EditorCommand.Scroll(
                            x = newScrollbar.x,
                            y = newScrollbar.y,
                            contentWidth = contentWidth,
                            contentHeight = contentHeight,
                            viewportWidth = currentViewport.width,
                            viewportHeight = currentViewport.height
                        )
                    )
                }
            }

            EditorMouseHandler(
                enabled = true,
                viewport = currentViewport,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                contentLayers = currentContentLayers,
                snapshot = state.editor.snapshot,
                gutterWidth = gutterWidth,
                scrollbar = state.scrollbar,
                execute = execute,
                focusRequester = focusRequester
            ) { mouseModifier ->
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
                    }.then(mouseModifier).drawWithCache {
                        val bounds = Rect.makeWH(w = size.width, h = size.height)

                        onDrawBehind {
                            if (!bounds.isEmpty) {
                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas

                                    nativeCanvas.save()

                                    nativeCanvas.clipRect(r = Rect.makeWH(w = bounds.width, h = bounds.height))

                                    nativeCanvas.clear(color = theme.backgroundColorPalette.backgroundColor)

                                    currentBackgroundLayer.render(canvas = nativeCanvas)

                                    currentHighlightedLineLayer?.render(canvas = nativeCanvas)

                                    currentGutterLineLayers.forEach { lineLayer ->
                                        lineLayer.render(canvas = nativeCanvas)
                                    }

                                    currentGutterSeparatorLayer.render(canvas = nativeCanvas)

                                    nativeCanvas.save()

                                    nativeCanvas.translate(dx = gutterWidth, dy = 0f)

                                    nativeCanvas.clipRect(
                                        r = Rect.makeWH(w = bounds.width - gutterWidth, h = bounds.height)
                                    )

                                    currentGuidelineLayer?.render(canvas = nativeCanvas)

                                    currentOccurrenceLayers.forEach { occurrenceLayer ->
                                        occurrenceLayer.render(canvas = nativeCanvas)
                                    }

                                    currentSelectionLayer.render(canvas = nativeCanvas)

                                    currentContentLayers.forEach { contentLayer ->
                                        contentLayer.render(canvas = nativeCanvas)
                                    }


                                    if (caretVisible) {
                                        currentCaretLayer?.render(canvas = nativeCanvas)
                                    }

                                    nativeCanvas.restore()

                                    nativeCanvas.restore()
                                }
                            }
                        }
                    }.pointerHoverIcon(PointerIcon(Cursor(Cursor.TEXT_CURSOR)))
                )
            }
        })
}