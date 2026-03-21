package io.github.numq.haskcore.feature.editor.presentation.mouse

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.presentation.feature.EditorCommand
import io.github.numq.haskcore.feature.editor.presentation.scrollbar.Scrollbar
import io.github.numq.haskcore.feature.editor.presentation.text.TextContentLayer
import io.github.numq.haskcore.feature.editor.presentation.viewport.Viewport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun EditorMouseHandler(
    enabled: Boolean,
    viewport: Viewport,
    contentWidth: Float,
    contentHeight: Float,
    contentLayers: List<TextContentLayer>,
    snapshot: TextSnapshot,
    gutterWidth: Float,
    scrollbar: Scrollbar,
    focusRequester: FocusRequester,
    execute: suspend (EditorCommand) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scope = rememberCoroutineScope()

    val currentViewport by rememberUpdatedState(viewport)

    val currentScrollbar by rememberUpdatedState(scrollbar)

    val currentContentLayers by rememberUpdatedState(contentLayers)

    val currentSnapshot by rememberUpdatedState(snapshot)

    var draggingMousePosition by remember { mutableStateOf<Offset?>(null) }

    val selectionCommands = remember { Channel<EditorCommand.TextSelection>(Channel.CONFLATED) }

    LaunchedEffect(selectionCommands) {
        for (command in selectionCommands) {
            execute(command)
        }
    }

    LaunchedEffect(draggingMousePosition) {
        while (isActive) {
            val position = draggingMousePosition ?: break

            val currentY = position.y

            val scrollThreshold = 60f

            val scrollSpeed = 20f

            val viewportHeight = currentViewport.height

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
                val nextY = (currentScrollbar.y + delta).coerceIn(
                    0f, (contentHeight - viewportHeight).coerceAtLeast(0f)
                )

                execute(
                    EditorCommand.Scroll(
                        x = currentScrollbar.x,
                        y = nextY,
                        contentWidth = contentWidth,
                        contentHeight = contentHeight,
                        viewportWidth = currentViewport.width,
                        viewportHeight = viewportHeight
                    )
                )

                val newPosition = calculatePositionAtOffset(
                    offset = position,
                    viewport = currentViewport,
                    contentLayers = currentContentLayers,
                    gutterWidth = gutterWidth,
                    scrollX = currentScrollbar.x,
                    snapshot = currentSnapshot
                )

                selectionCommands.trySend(EditorCommand.TextSelection.Extend(position = newPosition))
            }

            delay(10L)
        }
    }

    val mouseModifier = Modifier.pointerInput(enabled) {
        if (enabled) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    focusRequester.requestFocus()

                    val position = calculatePositionAtOffset(
                        offset = down.position,
                        viewport = currentViewport,
                        contentLayers = currentContentLayers,
                        gutterWidth = gutterWidth,
                        scrollX = currentScrollbar.x,
                        snapshot = currentSnapshot
                    )

                    scope.launch { execute(EditorCommand.MoveCaret(position = position)) }

                    selectionCommands.trySend(EditorCommand.TextSelection.Start(position = position))

                    draggingMousePosition = down.position

                    down.consume()

                    var dragEvent = down

                    while (true) {
                        val event = awaitPointerEvent()

                        if (!event.changes.any(PointerInputChange::pressed)) {
                            draggingMousePosition = null

                            break
                        }

                        val currentDrag = event.changes.firstOrNull()

                        if (currentDrag != null && currentDrag.position != dragEvent.position) {
                            dragEvent = currentDrag

                            draggingMousePosition = currentDrag.position

                            val currentPosition = calculatePositionAtOffset(
                                offset = currentDrag.position,
                                viewport = currentViewport,
                                contentLayers = currentContentLayers,
                                gutterWidth = gutterWidth,
                                scrollX = currentScrollbar.x,
                                snapshot = currentSnapshot
                            )
                            selectionCommands.trySend(EditorCommand.TextSelection.Extend(position = currentPosition))

                            currentDrag.consume()
                        }
                    }
                }
            }
        }
    }

    content(mouseModifier)
}

private fun calculatePositionAtOffset(
    offset: Offset,
    viewport: Viewport,
    contentLayers: List<TextContentLayer>,
    gutterWidth: Float,
    scrollX: Float,
    snapshot: TextSnapshot
) = when (val clickedViewportLine = viewport.viewportLines.find { viewportLine ->
    offset.y >= viewportLine.y && offset.y <= (viewportLine.y + viewportLine.height)
}) {
    null -> {
        val firstLineY = viewport.viewportLines.firstOrNull()?.y ?: 0f

        when {
            offset.y < firstLineY -> TextPosition.ZERO

            else -> {
                val lastLine = (snapshot.lines - 1).coerceAtLeast(0)

                val lineLength = snapshot.getLineLength(line = lastLine)

                TextPosition(line = lastLine, column = lineLength)
            }
        }
    }

    else -> {
        val targetLineIndex = clickedViewportLine.line

        val clickedLayer = contentLayers.find { contentLayer -> contentLayer.viewportLine.line == targetLineIndex }

        val column = clickedLayer?.getOffsetAtCoordinate(
            targetX = (offset.x - gutterWidth + scrollX).coerceAtLeast(0f)
        ) ?: snapshot.getLineLength(line = targetLineIndex)

        TextPosition(line = targetLineIndex, column = column)
    }
}