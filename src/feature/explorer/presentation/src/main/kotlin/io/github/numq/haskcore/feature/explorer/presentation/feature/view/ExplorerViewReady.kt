package io.github.numq.haskcore.feature.explorer.presentation.feature.view

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.overlay.menu.ContextMenu
import io.github.numq.haskcore.common.presentation.overlay.menu.ContextMenuItem
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerPosition
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState
import io.github.numq.haskcore.feature.explorer.presentation.menu.ContextMenuState
import io.github.numq.haskcore.feature.explorer.presentation.node.ExplorerNodeItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
internal fun ExplorerViewReady(
    state: ExplorerState.Ready, execute: suspend (ExplorerCommand) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.explorerTree.position.index,
        initialFirstVisibleItemScrollOffset = state.explorerTree.position.offset
    )

    val scrollbarAdapter = rememberScrollbarAdapter(listState)

    DisposableEffect(Unit) {
        onDispose {
            val position = ExplorerPosition(
                index = listState.firstVisibleItemIndex, offset = listState.firstVisibleItemScrollOffset
            )

            if (position != state.explorerTree.position) {
                scope.launch {
                    withContext(NonCancellable) {
                        execute(ExplorerCommand.SaveExplorerPosition(position = position))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            ExplorerPosition(
                index = listState.firstVisibleItemIndex, offset = listState.firstVisibleItemScrollOffset
            )
        }.distinctUntilChanged().conflate().debounce(500.milliseconds).filterNot(state.explorerTree.position::equals)
            .collect { position ->
                execute(ExplorerCommand.SaveExplorerPosition(position = position))
            }
    }

    val selectedPaths = remember(state.selectedNodes) {
        state.selectedNodes.map(ExplorerNode::path).toSet()
    }

    Container {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Explorer",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    items(items = state.explorerTree.nodes, key = ExplorerNode::path) { node ->
                        val menuState = state.contextMenuState

                        ContextMenu(
                            offset = when (menuState) {
                                is ContextMenuState.Visible if menuState.node.path == node.path -> Offset(
                                    x = menuState.x, y = menuState.y
                                )

                                else -> Offset.Unspecified
                            }, onOpen = { (x, y) ->
                                scope.launch {
                                    execute(ExplorerCommand.Menu.Open(node = node, x = x, y = y))
                                }
                            }, onClose = {
                                scope.launch {
                                    execute(ExplorerCommand.Menu.Close)
                                }
                            }, items = {
                                listOf(
                                    ContextMenuItem(
                                        label = "Create File",
                                        leadingIcon = Icons.AutoMirrored.Filled.NoteAdd,
                                        onClick = {
                                            scope.launch {
                                                execute(
                                                    ExplorerCommand.Menu.Action.CreateFile.ShowDialog(
                                                        node = node
                                                    )
                                                )
                                            }
                                        }), ContextMenuItem(
                                        label = "Create Directory",
                                        leadingIcon = Icons.Rounded.CreateNewFolder,
                                        onClick = {
                                            scope.launch {
                                                execute(
                                                    ExplorerCommand.Menu.Action.CreateDirectory.ShowDialog(
                                                        node = node
                                                    )
                                                )
                                            }
                                        }), ContextMenuItem(
                                        label = "Cut", leadingIcon = Icons.Rounded.ContentCut, onClick = {
                                            scope.launch {
                                                execute(ExplorerCommand.Menu.Action.Cut(node = node))
                                            }
                                        }), ContextMenuItem(
                                        label = "Copy", leadingIcon = Icons.Rounded.ContentCopy, onClick = {
                                            scope.launch {
                                                execute(ExplorerCommand.Menu.Action.Copy(node = node))
                                            }
                                        }), ContextMenuItem(
                                        label = "Paste", leadingIcon = Icons.Rounded.ContentPaste, onClick = {
                                            scope.launch {
                                                execute(ExplorerCommand.Menu.Action.Paste(node = node))
                                            }
                                        }), ContextMenuItem(
                                        label = "Delete", leadingIcon = Icons.Rounded.Delete, onClick = {
                                            scope.launch {
                                                execute(ExplorerCommand.Menu.Action.Delete(node = node))
                                            }
                                        })
                                )
                            }) {
                            ExplorerNodeItem(
                                node = node,
                                isSelected = node.path in selectedPaths,
                                toggleDirectoryExpansion = { node ->
                                    scope.launch {
                                        execute(ExplorerCommand.ToggleExplorerNode(node = node))
                                    }
                                },
                                select = { node ->
                                    scope.launch {
                                        execute(ExplorerCommand.SelectExplorerNode(node = node))
                                    }
                                },
                                openDocument = { node ->
                                    scope.launch {
                                        execute(ExplorerCommand.OpenPath(path = node.path))
                                    }
                                })
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = scrollbarAdapter,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    style = LocalScrollbarStyle.current.copy(
                        thickness = 8.dp,
                        unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .12f),
                        hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)
                    )
                )
            }
        }
    }
}