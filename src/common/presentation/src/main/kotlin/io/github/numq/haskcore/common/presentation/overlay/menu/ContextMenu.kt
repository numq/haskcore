package io.github.numq.haskcore.common.presentation.overlay.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContextMenu(
    state: ContextMenuState,
    onState: (ContextMenuState) -> Unit,
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit,
) {
    val currentOnState by rememberUpdatedState(onState)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().onPointerEvent(eventType = PointerEventType.Press, onEvent = { event ->
            if (event.button.isSecondary) {
                val (x, y) = event.changes.first().position

                currentOnState(ContextMenuState.Visible(x = x, y = y))
            }
        })) {
            content()
        }

        when (val currentState = state) {
            is ContextMenuState.Hidden -> Unit

            is ContextMenuState.Visible -> Popup(
                alignment = Alignment.TopStart, offset = IntOffset(
                    x = currentState.x.toInt(), y = currentState.y.toInt()
                ), onDismissRequest = {
                    currentOnState(ContextMenuState.Hidden)
                }) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 8.dp,
                    tonalElevation = 8.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
                    ) {
                        items().forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().alpha(
                                    alpha = when {
                                        item.enabled -> 1f

                                        else -> .5f
                                    }
                                ).clickable(
                                    enabled = item.enabled, onClick = {
                                        item.onClick()

                                        currentOnState(ContextMenuState.Hidden)
                                    }).padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    space = 12.dp, alignment = Alignment.Start
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item.leadingIcon?.let { imageVector ->
                                    Box(modifier = Modifier.size(20.dp)) {
                                        ContextMenuIcon(imageVector = imageVector)
                                    }
                                }

                                Text(
                                    text = item.label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Start,
                                    maxLines = 1
                                )

                                item.trailingIcon?.let { imageVector ->
                                    Box(modifier = Modifier.size(20.dp)) {
                                        ContextMenuIcon(imageVector = imageVector)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}