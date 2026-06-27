package io.github.numq.haskcore.common.presentation.overlay.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
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
    offset: Offset,
    onOpen: (Offset) -> Unit,
    onClose: () -> Unit,
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    val currentOnOpen by rememberUpdatedState(onOpen)

    val currentOnClose by rememberUpdatedState(onClose)

    Box(
        modifier = Modifier.fillMaxSize().onPointerEvent(
            eventType = PointerEventType.Press, onEvent = { event ->
                if (event.button.isSecondary) {
                    currentOnOpen(event.changes.first().position)
                }
            })
    ) {
        content()

        if (offset.isSpecified) {
            Popup(
                alignment = Alignment.TopStart, offset = IntOffset(
                    x = offset.x.toInt(), y = offset.y.toInt()
                ), onDismissRequest = currentOnClose
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 8.dp,
                    tonalElevation = 8.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            space = 4.dp, alignment = Alignment.CenterVertically
                        )
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

                                        currentOnClose()
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