package io.github.numq.haskcore.feature.workspace.presentation.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.presentation.tab.WorkspaceTabs
import io.github.numq.haskcore.platform.window.WindowDecoration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.skiko.Cursor
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalSplitPaneApi::class)
@Composable
fun WorkspaceView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    workspace: Workspace,
    icon: Painter,
    execution: @Composable () -> Unit,
    editor: @Composable (path: String?, content: @Composable (@Composable () -> Unit) -> Unit) -> Unit,
    output: @Composable () -> Unit,
    status: @Composable () -> Unit
) {
    if (projectScope.closed) return

    val feature = koinInject<WorkspaceFeature>(scope = projectScope) { parametersOf(workspace) }

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is WorkspaceEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val scope = rememberCoroutineScope()

    val windowState = rememberWindowState(
        placement = when {
            state.workspace.isFullscreen == true -> WindowPlacement.Fullscreen

            else -> WindowPlacement.Floating
        }, position = state.workspace.x?.let { x ->
            state.workspace.y?.let { y ->
                WindowPosition(x = x.dp, y = y.dp)
            }
        } ?: WindowPosition.PlatformDefault, size = state.workspace.width?.dp?.let { width ->
            state.workspace.height?.dp?.let { height ->
                DpSize(width = width, height = height)
            }
        } ?: DpSize(800.dp, 600.dp))

    DisposableEffect(windowState) {
        onDispose {
            val workspace = state.workspace

            val x = windowState.position.x.value

            val y = windowState.position.y.value

            val width = windowState.size.width.value

            val height = windowState.size.height.value

            val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

            scope.launch(NonCancellable) {
                if (x != workspace.x || y != workspace.y || width != workspace.width || height != workspace.height || isFullscreen != workspace.isFullscreen) {
                    feature.execute(
                        WorkspaceCommand.SaveDimensions(
                            x = x, y = y, width = width, height = height, isFullscreen = isFullscreen,
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(windowState) {
        snapshotFlow {
            Triple(windowState.position, windowState.size, windowState.placement)
        }.distinctUntilChanged().conflate().debounce(500.milliseconds).collect { (position, size, placement) ->
            val workspace = state.workspace

            val x = position.x.value

            val y = position.y.value

            val width = size.width.value

            val height = size.height.value

            val isFullscreen = placement == WindowPlacement.Fullscreen

            if (x != workspace.x || y != workspace.y || width != workspace.width || height != workspace.height || isFullscreen != workspace.isFullscreen) {
                feature.execute(
                    WorkspaceCommand.SaveDimensions(
                        x = position.x.value,
                        y = position.y.value,
                        width = size.width.value,
                        height = size.height.value,
                        isFullscreen = placement == WindowPlacement.Fullscreen
                    )
                )
            }
        }
    }

    WindowDecoration(
        title = state.workspace.name ?: state.workspace.path,
        icon = icon,
        windowDecorationHeight = 40.dp,
        windowState = windowState,
        minimumWindowSize = DpSize(width = 512.dp, height = 40.dp),
        onCloseRequest = {
            scope.launch {
                feature.execute(WorkspaceCommand.CloseWorkspace)
            }
        },
        titleContent = { windowDecorationColors ->
            val path = state.workspace.path

            val name = state.workspace.name

            when (name) {
                null -> Text(
                    text = path,
                    color = windowDecorationColors.title(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                else -> {
                    Text(
                        text = name,
                        color = windowDecorationColors.title(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "($path)",
                        color = windowDecorationColors.title().copy(alpha = .7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        controlsContent = {
            execution()
        },
        content = {
            val verticalSplitPaneState = rememberSplitPaneState(
                initialPositionPercentage = state.workspace.ratio
            )

            DisposableEffect(verticalSplitPaneState) {
                onDispose {
                    val ratio = verticalSplitPaneState.positionPercentage

                    if (ratio != state.workspace.ratio) {
                        scope.launch(NonCancellable) {
                            feature.execute(WorkspaceCommand.SaveRatio(ratio = verticalSplitPaneState.positionPercentage))
                        }
                    }
                }
            }

            LaunchedEffect(verticalSplitPaneState) {
                snapshotFlow {
                    verticalSplitPaneState.positionPercentage
                }.distinctUntilChanged().conflate().debounce(500.milliseconds).filterNot(state.workspace.ratio::equals)
                    .collect { ratio ->
                        feature.execute(WorkspaceCommand.SaveRatio(ratio = ratio))
                    }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VerticalSplitPane(modifier = Modifier.weight(1f), splitPaneState = verticalSplitPaneState) {
                    first {
                        editor(state.workspace.activeDocumentPath) { content ->
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top
                            ) {
                                WorkspaceTabs(
                                    documents = state.workspace.documents,
                                    activeDocumentPath = state.workspace.activeDocumentPath,
                                    selectDocument = { document ->
                                        scope.launch {
                                            feature.execute(WorkspaceCommand.OpenTab(path = document.path))
                                        }
                                    },
                                    closeDocument = { document ->
                                        scope.launch {
                                            feature.execute(WorkspaceCommand.CloseTab(path = document.path))
                                        }
                                    })
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    content()
                                }
                            }
                        }
                    }

                    splitter {
                        handle {
                            Box(
                                modifier = Modifier.markAsHandle().height(8.dp).fillMaxWidth().pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(
                                            Cursor.N_RESIZE_CURSOR
                                        )
                                    )
                                )
                            )
                        }
                    }

                    second(minSize = 32.dp) {
                        output()
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
                    status()
                }
            }
        })
}