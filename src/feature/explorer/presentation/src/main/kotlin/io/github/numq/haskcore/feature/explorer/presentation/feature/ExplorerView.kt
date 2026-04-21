package io.github.numq.haskcore.feature.explorer.presentation.feature

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.common.presentation.feature.Feature
import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerPosition
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import io.github.numq.haskcore.feature.explorer.presentation.node.ExplorerNodeItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun ExplorerView(
    feature: Feature<ExplorerState, ExplorerCommand, ExplorerEvent>,
    handleError: (Throwable) -> Unit,
    selectedPath: String?,
) {
    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is ExplorerEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        when (val explorerTree = state.explorerTree) {
            is ExplorerTree.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ExplorerTree.Loaded -> {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = explorerTree.position.index,
                    initialFirstVisibleItemScrollOffset = explorerTree.position.offset
                )

                val scrollbarAdapter = rememberScrollbarAdapter(listState)

                DisposableEffect(listState) {
                    onDispose {
                        val position = ExplorerPosition(
                            index = listState.firstVisibleItemIndex, offset = listState.firstVisibleItemScrollOffset
                        )

                        if (position != explorerTree.position) {
                            scope.launch(NonCancellable) {
                                feature.execute(ExplorerCommand.SaveExplorerPosition(position = position))
                            }
                        }
                    }
                }

                LaunchedEffect(listState) {
                    snapshotFlow {
                        ExplorerPosition(
                            index = listState.firstVisibleItemIndex, offset = listState.firstVisibleItemScrollOffset
                        )
                    }.distinctUntilChanged().conflate().debounce(500.milliseconds)
                        .filterNot(explorerTree.position::equals).collect { position ->
                            feature.execute(ExplorerCommand.SaveExplorerPosition(position = position))
                        }
                }

                val nodes = explorerTree.nodes

                LaunchedEffect(selectedPath, nodes) {
                    if (selectedPath != null) {
                        val index = nodes.indexOfFirst { node ->
                            node.path == selectedPath
                        }

                        if (index != -1) {
                            listState.animateScrollToItem(index)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(all = 12.dp)
                    ) {
                        items(
                            items = explorerTree.nodes, key = ExplorerNode::path, contentType = { it::class }) { node ->
                            ExplorerNodeItem(
                                node = node,
                                isSelected = node.path == state.selectedPath,
                                toggleDirectoryExpansion = { node ->
                                    scope.launch {
                                        feature.execute(ExplorerCommand.ToggleExplorerNode(node = node))
                                    }
                                },
                                select = { node ->
                                    scope.launch {
                                        feature.execute(ExplorerCommand.SelectExplorerNode(path = node.path))
                                    }
                                },
                                openDocument = { node ->
                                    scope.launch {
                                        feature.execute(ExplorerCommand.OpenPath(path = node.path))
                                    }
                                })
                        }
                    }

                    VerticalScrollbar(
                        adapter = scrollbarAdapter,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        style = LocalScrollbarStyle.current.copy(
                            shape = RectangleShape,
                            thickness = 8.dp,
                            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                        )
                    )
                }
            }
        }
    }
}