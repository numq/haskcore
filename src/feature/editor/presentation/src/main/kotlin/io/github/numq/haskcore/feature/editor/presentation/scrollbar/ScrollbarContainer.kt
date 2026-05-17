package io.github.numq.haskcore.feature.editor.presentation.scrollbar

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScrollbarContainer(
    modifier: Modifier,
    scrollbar: Scrollbar,
    scroll: (x: Float, y: Float, viewportWidth: Float, viewportHeight: Float) -> Unit,
    contentWidth: Float,
    contentHeight: Float,
    paddingStart: Float,
    minimalHeight: Float,
    thickness: Float,
    content: @Composable (viewportWidth: Float, viewportHeight: Float) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val thicknessDp = with(density) { thickness.toDp() }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopStart) {
        val fullWidth = constraints.maxWidth.toFloat()

        val fullHeight = constraints.maxHeight.toFloat()

        val isHorizontalScrollable by remember(contentWidth, fullWidth) {
            derivedStateOf {
                contentWidth > fullWidth
            }
        }

        val isVerticalScrollable by remember(contentHeight, fullHeight) {
            derivedStateOf {
                contentHeight > fullHeight
            }
        }

        Box(modifier = Modifier.fillMaxSize().onPointerEvent(PointerEventType.Scroll) { event ->
            val delta = event.changes.first().scrollDelta

            val isShiftPressed = event.keyboardModifiers.isShiftPressed

            when {
                isHorizontalScrollable && isShiftPressed -> scope.launch {
                    val scrollDelta = (delta.y + delta.x) * 64f

                    val maxScrollX = (contentWidth - fullWidth).coerceAtLeast(0f)

                    val x = (scrollbar.x + scrollDelta).coerceIn(0f, maxScrollX)

                    scroll(x, scrollbar.y, fullWidth, fullHeight)
                }

                isVerticalScrollable -> scope.launch {
                    val scrollDelta = delta.y * 64f

                    val maxScrollY = (contentHeight - fullHeight).coerceAtLeast(0f)

                    val y = (scrollbar.y + scrollDelta).coerceIn(0f, maxScrollY)

                    scroll(scrollbar.x, y, fullWidth, fullHeight)
                }
            }
        }, contentAlignment = Alignment.TopStart) {
            content(fullWidth, fullHeight)

            val hAdapter = remember(scrollbar.x, contentWidth, fullWidth) {
                object : ScrollbarAdapter {
                    override val scrollOffset = scrollbar.x.toDouble()

                    override val contentSize = contentWidth.toDouble()

                    override val viewportSize = fullWidth.toDouble()

                    override suspend fun scrollTo(scrollOffset: Double) {
                        scroll(scrollOffset.toFloat(), scrollbar.y, fullWidth, fullHeight)
                    }
                }
            }

            val vAdapter = remember(scrollbar.y, contentHeight, fullHeight) {
                object : ScrollbarAdapter {
                    override val scrollOffset = scrollbar.y.toDouble()

                    override val contentSize = contentHeight.toDouble()

                    override val viewportSize = fullHeight.toDouble()

                    override suspend fun scrollTo(scrollOffset: Double) {
                        scroll(scrollbar.x, scrollOffset.toFloat(), fullWidth, fullHeight)
                    }
                }
            }

            if (isHorizontalScrollable) {
                HorizontalScrollbar(
                    adapter = hAdapter,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = with(density) {
                        paddingStart.toDp()
                    }, end = thicknessDp),
                    style = LocalScrollbarStyle.current.copy(
                        thickness = thicknessDp,
                        minimalHeight = with(density) { minimalHeight.toDp() },
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .12f),
                        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)
                    )
                )
            }

            if (isVerticalScrollable) {
                VerticalScrollbar(
                    adapter = vAdapter,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    style = LocalScrollbarStyle.current.copy(
                        thickness = thicknessDp,
                        minimalHeight = with(density) { minimalHeight.toDp() },
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .12f),
                        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)
                    )
                )
            }
        }
    }
}