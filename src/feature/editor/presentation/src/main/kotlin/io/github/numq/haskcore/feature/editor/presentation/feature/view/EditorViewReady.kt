package io.github.numq.haskcore.feature.editor.presentation.feature.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.editor.presentation.documentation.DocumentationPopup
import io.github.numq.haskcore.feature.editor.presentation.documentation.DocumentationState
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorState
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.editor.presentation.measurements.Measurements
import io.github.numq.haskcore.feature.editor.presentation.menu.ContextMenu
import io.github.numq.haskcore.feature.editor.presentation.mouse.EditorMouseHandler
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.ScrollbarContainer
import io.github.numq.haskcore.feature.editor.presentation.suggestions.SuggestionPopup
import io.github.numq.haskcore.feature.editor.presentation.suggestions.SuggestionsState
import io.github.numq.haskcore.feature.editor.presentation.viewport.ViewportCalculator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import java.awt.Cursor
import java.awt.event.KeyEvent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EditorViewReady(
    state: EditorState.Ready,
    font: Font,
    theme: EditorTheme,
    layerFactory: LayerFactory,
    execute: suspend (EditorCommand) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val focusRequester = remember {
        FocusRequester()
    }

    var isFocused by remember {
        mutableStateOf(false)
    }

    val gutterWidth by remember(state.editor.snapshot.lines, font.lineHeight) {
        derivedStateOf {
            val maxTextWidth = font.measureTextWidth("${state.editor.snapshot.lines + 1}")

            Measurements.GUTTER_PADDING_START + maxTextWidth + Measurements.GUTTER_GAP + font.lineHeight + Measurements.GUTTER_PADDING_END
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
        content = { viewportWidth, viewportHeight ->
            var caretVisible by remember { mutableStateOf(true) }

            LaunchedEffect(state.editor.caret.position, isFocused) {
                if (!isFocused) {
                    caretVisible = false

                    return@LaunchedEffect
                }

                caretVisible = true

                while (isActive) {
                    delay(530L.milliseconds)

                    caretVisible = !caretVisible
                }
            }

            val viewport by remember(state.editor.snapshot, state.scrollbar.y, viewportWidth, viewportHeight, font) {
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

            val backgroundOutlineLayer by remember(currentViewport.width, theme) {
                derivedStateOf {
                    layerFactory.createBackgroundOutlineLayer(
                        width = currentViewport.width, theme = theme
                    )
                }
            }

            val currentBackgroundOutlineLayer by rememberUpdatedState(backgroundOutlineLayer)

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

            val density = LocalDensity.current

            val foldingRotation by animateFloatAsState(
                when {
                    true -> 90f // todo check is folding region

                    else -> 0f
                }
            )

            val foldingIconPainter = rememberVectorPainter(image = Icons.Rounded.ChevronRight)

            val foldingIconColor = MaterialTheme.colorScheme.onSurfaceVariant

            val gutterActionImage = remember(density, foldingIconColor, foldingRotation) {
                val iconSizeDp = with(density) { (font.lineHeight * .9f).toDp() }

                val iconSize = with(density) { iconSizeDp.toPx() }.toInt()

                val iconColor = foldingIconColor

                val bitmap = ImageBitmap(iconSize, iconSize)

                val canvas = Canvas(bitmap)

                val scope = CanvasDrawScope()

                scope.draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = canvas,
                    size = Size(iconSize.toFloat(), iconSize.toFloat())
                ) {
                    with(foldingIconPainter) {
                        rotate(foldingRotation) {
                            draw(
                                size = Size(iconSize.toFloat(), iconSize.toFloat()),
                                colorFilter = ColorFilter.tint(iconColor)
                            )
                        }
                    }
                }

                bitmap.asSkiaBitmap().use(Image::makeFromBitmap)
            }

            DisposableEffect(gutterActionImage) {
                onDispose {
                    if (!gutterActionImage.isClosed) {
                        gutterActionImage.close()
                    }
                }
            }

            val gutterActionLayers by remember(currentViewport.viewportLines, gutterActionImage, gutterWidth, theme) {
                derivedStateOf {
                    layerFactory.createGutterActionLayers(
                        viewportLines = currentViewport.viewportLines,
                        image = gutterActionImage,
                        gutterWidth = gutterWidth,
                        theme = theme
                    )
                }
            }

            val currentGutterActionLayers by rememberUpdatedState(gutterActionLayers) // todo

            val gutterSeparatorLayer by remember(currentViewport.height, gutterWidth, theme) {
                derivedStateOf {
                    layerFactory.createGutterSeparatorLayer(
                        x = gutterWidth, height = currentViewport.height, theme = theme
                    )
                }
            }

            val currentGutterSeparatorLayer by rememberUpdatedState(gutterSeparatorLayer)

            val guidelineLayer by remember(
                state.editor.language, currentViewport.height, state.scrollbar.x, font, theme
            ) {
                derivedStateOf {
                    state.editor.language.guidelineColumn?.let { column ->
                        layerFactory.createGuidelineLayer(
                            column = column,
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
                currentViewport.viewportLines,
                state.analysis?.tokensPerLine,
                state.syntax?.tokensPerLine,
                state.scrollbar.x,
                font,
                theme
            ) {
                derivedStateOf {
                    layerFactory.createCodeAreaContentLayers(
                        viewportLines = currentViewport.viewportLines,
                        semanticTokensPerLine = state.analysis?.tokensPerLine,
                        syntaxTokensPerLine = state.syntax?.tokensPerLine,
                        scrollX = state.scrollbar.x,
                        font = font,
                        theme = theme
                    )
                }
            }

            val currentContentLayers by rememberUpdatedState(contentLayers)

            val viewportRange by remember {
                derivedStateOf {
                    when {
                        currentContentLayers.isEmpty() -> null

                        else -> {
                            val start = currentContentLayers.firstOrNull()?.viewportLine?.line ?: 0

                            val end = currentContentLayers.lastOrNull()?.viewportLine?.line ?: 0

                            start to end
                        }
                    }
                }
            }

            LaunchedEffect(viewportRange) {
                viewportRange?.let { (start, end) ->
                    execute(EditorCommand.UpdateViewport(start = start, end = end))
                }
            }

            val occurrenceLayers by remember(
                state.editor.caret, state.syntax, state.scrollbar.x, currentContentLayers, theme
            ) {
                derivedStateOf {
                    when (val highlighting = state.syntax) {
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

            val issueLayers by remember(state.analysis, state.scrollbar.x, currentContentLayers, theme) {
                derivedStateOf {
                    state.analysis?.let { analysis ->
                        layerFactory.createIssueLayers(
                            contentLayers = currentContentLayers,
                            issues = analysis.issues,
                            scrollX = state.scrollbar.x,
                            theme = theme
                        )
                    } ?: emptyList()
                }
            }

            val currentIssueLayers by rememberUpdatedState(issueLayers)

            val selectionLayer by remember(state.editor.selection, state.scrollbar.x, currentContentLayers, theme) {
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

            val caretLayer by remember(state.editor.caret, state.scrollbar.x, currentContentLayers, font, theme) {
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

            val caretOffset by remember(
                state.editor.caret, state.scrollbar.x, currentContentLayers, gutterWidth, font, density
            ) {
                derivedStateOf {
                    currentContentLayers.find { currentCaretLayer ->
                        currentCaretLayer.viewportLine.line == state.editor.caret.position.line
                    }?.let { caretLineLayer ->
                        val xOffset = caretLineLayer.getCoordinateAtOffset(
                            offset = state.editor.caret.position.column
                        ).takeIf { offset ->
                            offset > 0f || state.editor.caret.position.column == 0
                        } ?: (state.editor.caret.position.column * font.charWidth)

                        val xPx = gutterWidth + xOffset - state.scrollbar.x + Measurements.EDITOR_PADDING_START

                        val yPx = caretLineLayer.viewportLine.textBaselineY + font.descent + 2f

                        with(density) {
                            Offset(xPx, yPx)
                        }
                    }
                }
            }

            LaunchedEffect(
                state.editor.caret.position, currentViewport.width, currentViewport.height, gutterWidth, font
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
                charWidth = font.charWidth,
                focusRequester = focusRequester
            ) { mouseModifier ->
                ContextMenu(menu = state.menu, openMenu = { (x, y) ->
                    scope.launch {
                        execute(EditorCommand.Menu.Open(x = x, y = y))
                    }
                }, closeMenu = {
                    scope.launch {
                        execute(EditorCommand.Menu.Close)
                    }
                }, runStack = {
                    scope.launch {
                        execute(EditorCommand.Menu.RunStack)
                    }
                }, runCabal = {
                    scope.launch {
                        execute(EditorCommand.Menu.RunStack)
                    }
                }, runGhc = {
                    scope.launch {
                        execute(EditorCommand.Menu.RunStack)
                    }
                }, cut = {
                    scope.launch {
                        execute(EditorCommand.ProcessKey(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK, 0))
                    }
                }, copy = {
                    scope.launch {
                        execute(EditorCommand.ProcessKey(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, 0))
                    }
                }, paste = {
                    scope.launch {
                        execute(EditorCommand.ProcessKey(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, 0))
                    }
                }, selectAll = {
                    scope.launch {
                        execute(EditorCommand.ProcessKey(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK, 0))
                    }
                }, content = {
                    Box(
                        modifier = Modifier.fillMaxSize().focusRequester(focusRequester).onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    }.focusable().onKeyEvent { keyEvent ->
                        when (keyEvent.type) {
                            KeyEventType.KeyDown -> {
                                keyEvent.awtEventOrNull?.let { awtEvent ->
                                    val suggestionState = state.suggestionsState

                                    if (suggestionState is SuggestionsState.Visible) {
                                        val suggestions = suggestionState.suggestions

                                        val selectedIndex = suggestionState.selectedIndex

                                        when (awtEvent.keyCode) {
                                            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
                                                scope.launch {
                                                    execute(EditorCommand.DismissSuggestions)
                                                }

                                                return@onKeyEvent false
                                            }

                                            KeyEvent.VK_DOWN -> {
                                                val newIndex = (selectedIndex + 1) % suggestions.size

                                                scope.launch {
                                                    execute(EditorCommand.UpdateSuggestionsSelection(index = newIndex))
                                                }

                                                return@onKeyEvent true
                                            }

                                            KeyEvent.VK_UP -> {
                                                val newIndex = (selectedIndex - 1 + suggestions.size) % suggestions.size

                                                scope.launch {
                                                    execute(EditorCommand.UpdateSuggestionsSelection(index = newIndex))
                                                }

                                                return@onKeyEvent true
                                            }

                                            KeyEvent.VK_ENTER, KeyEvent.VK_TAB -> {
                                                suggestions.getOrNull(selectedIndex)?.let { suggestion ->
                                                    scope.launch {
                                                        execute(EditorCommand.ApplySuggestion(suggestion = suggestion))
                                                    }
                                                }

                                                return@onKeyEvent true
                                            }

                                            KeyEvent.VK_ESCAPE -> {
                                                scope.launch {
                                                    joinAll(launch {
                                                        execute(EditorCommand.DismissDocumentation)
                                                    }, launch {
                                                        execute(EditorCommand.DismissSuggestions)
                                                    })
                                                }

                                                return@onKeyEvent true
                                            }
                                        }
                                    }

                                    scope.launch {
                                        execute(
                                            EditorCommand.ProcessKey(
                                                keyCode = awtEvent.keyCode,
                                                modifiers = awtEvent.modifiersEx and (KeyEvent.SHIFT_DOWN_MASK or KeyEvent.CTRL_DOWN_MASK or KeyEvent.META_DOWN_MASK or KeyEvent.ALT_DOWN_MASK or KeyEvent.ALT_GRAPH_DOWN_MASK),
                                                utf16CodePoint = keyEvent.utf16CodePoint,
                                                offset = caretOffset
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

                                    currentBackgroundOutlineLayer.render(canvas = nativeCanvas)

                                    currentHighlightedLineLayer?.render(canvas = nativeCanvas)

                                    currentGutterLineLayers.forEach { lineLayer ->
                                        lineLayer.render(canvas = nativeCanvas)
                                    }

//                                    currentGutterActionLayers.forEach { actionLayer ->
//                                        actionLayer.render(canvas = nativeCanvas) // todo implement folding action
//                                    }

                                    currentGutterSeparatorLayer.render(canvas = nativeCanvas)

                                    nativeCanvas.save()

                                    nativeCanvas.translate(dx = gutterWidth, dy = 0f)

                                    nativeCanvas.clipRect(
                                        r = Rect.makeWH(w = bounds.width - gutterWidth, h = bounds.height)
                                    )

                                    currentGuidelineLayer?.render(canvas = nativeCanvas)

                                    if (state.editor.selection.range.isEmpty) {
                                        currentOccurrenceLayers.forEach { occurrenceLayer ->
                                            occurrenceLayer.render(canvas = nativeCanvas)
                                        }
                                    }

                                    currentSelectionLayer.render(canvas = nativeCanvas)

                                    currentContentLayers.forEach { contentLayer ->
                                        contentLayer.render(canvas = nativeCanvas)
                                    }

                                    currentIssueLayers.forEach { issueLayer ->
                                        issueLayer.render(canvas = nativeCanvas)
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
                })
            }

            if (isFocused) {
                (state.suggestionsState as? SuggestionsState.Visible)?.let { suggestionsState ->
                    SuggestionPopup(
                        suggestionsState = suggestionsState,
                        theme = theme,
                        applySuggestion = { suggestion ->
                            scope.launch {
                                execute(EditorCommand.ApplySuggestion(suggestion = suggestion))
                            }
                        },
                        dismiss = {
                            scope.launch {
                                execute(EditorCommand.DismissSuggestions)
                            }
                        })
                }

                (state.documentationState as? DocumentationState.Visible)?.let { documentationState ->
                    val offset by remember(currentViewport.viewportLines, documentationState.position.line) {
                        derivedStateOf {
                            val viewportLine = currentViewport.viewportLines.find { viewportLine ->
                                viewportLine.line == documentationState.position.line
                            }

                            val x = documentationState.offset.x

                            val y = when (viewportLine) {
                                null -> documentationState.offset.y

                                else -> viewportLine.y + viewportLine.height
                            }

                            Offset(x = x, y = y)
                        }
                    }

                    DocumentationPopup(
                        documentationState = documentationState.copy(offset = offset),
                        theme = theme,
                        mouseEnter = {
                            scope.launch {
                                execute(
                                    EditorCommand.DocumentationHover.Enter(
                                        position = state.documentationState.position,
                                        offset = state.documentationState.offset
                                    )
                                )
                            }
                        },
                        mouseExit = {
                            scope.launch {
                                execute(EditorCommand.DocumentationHover.Exit)
                            }
                        },
                        dismiss = {
                            scope.launch {
                                execute(EditorCommand.DismissDocumentation)
                            }
                        })
                }
            }
        })
}