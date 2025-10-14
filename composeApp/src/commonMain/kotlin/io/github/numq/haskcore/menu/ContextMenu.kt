package io.github.numq.haskcore.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
internal fun ContextMenu(
    state: ContextMenuState, actions: Set<ContextMenuAction>, close: () -> Unit, content: @Composable () -> Unit,
) {
    BoxWithConstraints {
        val minSize by remember(constraints.maxWidth, constraints.maxHeight) {
            derivedStateOf {
                DpSize((constraints.maxWidth * .1f).dp, (constraints.maxHeight * .1f).dp)
            }
        }

        val maxSize by remember(constraints.maxWidth, constraints.maxHeight) {
            derivedStateOf {
                DpSize((constraints.maxWidth * .9f).dp, (constraints.maxHeight * .9f).dp)
            }
        }

        content()

        if (state is ContextMenuState.Visible<*>) {
            Popup(
                alignment = Alignment.TopStart,
                offset = state.offset.let { (x, y) -> IntOffset(x = x.toInt(), y = y.toInt()) },
                onDismissRequest = close
            ) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max).sizeIn(
                            minWidth = minSize.width,
                            minHeight = minSize.height,
                            maxWidth = maxSize.width,
                            maxHeight = maxSize.height
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        actions.forEachIndexed { index, action ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    action.click()

                                    close()
                                }, contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = action.label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (index != actions.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}