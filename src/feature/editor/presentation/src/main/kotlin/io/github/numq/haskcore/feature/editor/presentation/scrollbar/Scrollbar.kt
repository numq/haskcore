package io.github.numq.haskcore.feature.editor.presentation.scrollbar

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Scrollbar(
    modifier: Modifier,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
    contentWidth: Float,
    contentHeight: Float,
    paddingStart: Float,
    minimalHeight: Float,
    thickness: Float,
    theme: EditorTheme,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val thicknessDp = with(density) { thickness.toDp() }

    val edgePadding = with(density) { 2f.toDp() }

    val gap = with(density) { 4f.toDp() }

    BoxWithConstraints(modifier = modifier.padding(edgePadding).onPointerEvent(PointerEventType.Scroll) { event ->
        val delta = event.changes.first().scrollDelta

        val isShiftPressed = event.keyboardModifiers.isShiftPressed

        scope.launch {
            when {
                isShiftPressed -> {
                    val scrollDelta = (delta.y + delta.x) * 64f

                    val targetValue = (horizontalScrollState.value + scrollDelta).coerceIn(
                        0f, horizontalScrollState.maxValue.toFloat()
                    )

                    horizontalScrollState.scrollTo(targetValue.toInt())
                }

                else -> {
                    val scrollDelta = delta.y * 64f

                    val targetValue =
                        (verticalScrollState.value + scrollDelta).coerceIn(0f, verticalScrollState.maxValue.toFloat())

                    verticalScrollState.scrollTo(targetValue.toInt())
                }
            }
        }
    }) {
        val fullWidth = constraints.maxWidth.toFloat()

        val fullHeight = constraints.maxHeight.toFloat()

        content(this)

        val hAdapter = remember(horizontalScrollState, contentWidth, fullWidth) {
            object : ScrollbarAdapter {
                override val scrollOffset get() = horizontalScrollState.value.toDouble()

                override val contentSize get() = contentWidth.toDouble()

                override val viewportSize get() = fullWidth.toDouble()

                override suspend fun scrollTo(scrollOffset: Double) {
                    horizontalScrollState.scrollTo(scrollOffset.toInt())
                }
            }
        }

        val vAdapter = remember(verticalScrollState, contentHeight, fullHeight) {
            object : ScrollbarAdapter {
                override val scrollOffset get() = verticalScrollState.value.toDouble()

                override val contentSize get() = contentHeight.toDouble()

                override val viewportSize get() = fullHeight.toDouble()

                override suspend fun scrollTo(scrollOffset: Double) {
                    verticalScrollState.scrollTo(scrollOffset.toInt())
                }
            }
        }

        HorizontalScrollbar(
            adapter = hAdapter,
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(start = with(density) { paddingStart.toDp() }, end = thicknessDp),
            style = LocalScrollbarStyle.current.copy(
                thickness = thicknessDp,
                minimalHeight = with(density) { minimalHeight.toDp() },
                shape = RectangleShape,
                unhoverColor = Color(theme.scrollbarColorPalette.backgroundColor).copy(alpha = .5f),
                hoverColor = Color(theme.scrollbarColorPalette.hoverColor)
            )
        )

        VerticalScrollbar(
            adapter = vAdapter,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = LocalScrollbarStyle.current.copy(
                thickness = thicknessDp,
                minimalHeight = with(density) { minimalHeight.toDp() },
                shape = RectangleShape,
                unhoverColor = Color(theme.scrollbarColorPalette.backgroundColor).copy(alpha = .5f),
                hoverColor = Color(theme.scrollbarColorPalette.hoverColor)
            )
        )
    }
}