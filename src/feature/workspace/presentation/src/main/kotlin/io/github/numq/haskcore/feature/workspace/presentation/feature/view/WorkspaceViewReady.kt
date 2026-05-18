package io.github.numq.haskcore.feature.workspace.presentation.feature.view

import androidx.compose.foundation.background
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
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.tab.CloseableTabs
import io.github.numq.haskcore.common.presentation.window.WindowDecoration
import io.github.numq.haskcore.feature.workspace.core.WorkspaceDocument
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceCommand
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceState
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContent
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContentHandle
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfToolContent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalSplitPaneApi::class)
@Composable
internal fun WorkspaceViewReady(
    state: WorkspaceState.Ready,
    execute: suspend (WorkspaceCommand) -> Unit,
    icon: Painter,
    execution: @Composable () -> Unit,
    explorer: @Composable (path: String?) -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?) -> Unit,
    output: @Composable () -> Unit,
    status: @Composable () -> Unit,
) {
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
                    execute(
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
                execute(
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
        windowDecorationHeight = 48.dp,
        windowState = windowState,
        minimumWindowSize = DpSize(width = 512.dp, height = 48.dp),
        onCloseRequest = {
            scope.launch {
                execute(WorkspaceCommand.ExitApplication)
            }
        },
        titleIcon = icon,
        titleContent = { windowDecorationColors ->
            val path = state.workspace.path

            val name = state.workspace.name

            when (name) {
                null -> Text(
                    text = "C:\\Users\\User\\Documents\\HaskellProject", // path, todo
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
            val leftRatioChannel = remember { Channel<Float>(Channel.CONFLATED) }

            DisposableEffect(leftRatioChannel) {
                onDispose {
                    leftRatioChannel.close()
                }
            }

            LaunchedEffect(leftRatioChannel) {
                leftRatioChannel.consumeAsFlow().debounce(500.milliseconds).collect { ratio ->
                    execute(WorkspaceCommand.SaveLeftShelfPanelRatio(ratio = ratio))
                }
            }

            var localLeftRatio by remember(state.workspace.shelf.leftPanel.ratio) {
                mutableFloatStateOf(state.workspace.shelf.leftPanel.ratio)
            }

            val rightRatioChannel = remember { Channel<Float>(Channel.CONFLATED) }

            DisposableEffect(rightRatioChannel) {
                onDispose {
                    rightRatioChannel.close()
                }
            }

            LaunchedEffect(rightRatioChannel) {
                rightRatioChannel.consumeAsFlow().debounce(500.milliseconds).collect { ratio ->
                    execute(WorkspaceCommand.SaveRightShelfPanelRatio(ratio = ratio))
                }
            }

            var localRightRatio by remember(state.workspace.shelf.rightPanel.ratio) {
                mutableFloatStateOf(state.workspace.shelf.rightPanel.ratio)
            }

            val verticalSplitPaneState = rememberSplitPaneState(
                initialPositionPercentage = state.workspace.verticalRatio
            )

            DisposableEffect(verticalSplitPaneState) {
                onDispose {
                    val ratio = verticalSplitPaneState.positionPercentage

                    if (ratio != state.workspace.verticalRatio) {
                        scope.launch(NonCancellable) {
                            execute(WorkspaceCommand.SaveVerticalRatio(ratio = verticalSplitPaneState.positionPercentage))
                        }
                    }
                }
            }

            LaunchedEffect(verticalSplitPaneState) {
                snapshotFlow {
                    verticalSplitPaneState.positionPercentage
                }.distinctUntilChanged().conflate().debounce(500.milliseconds)
                    .filterNot(state.workspace.verticalRatio::equals).collect { ratio ->
                        execute(WorkspaceCommand.SaveVerticalRatio(ratio = ratio))
                    }
            }

            val leftWeight by remember(localLeftRatio, state.workspace.shelf.leftPanel.activeTool) {
                derivedStateOf {
                    localLeftRatio.takeIf {
                        state.workspace.shelf.leftPanel.activeTool != null
                    } ?: 0f
                }
            }

            val rightWeight by remember(localRightRatio, state.workspace.shelf.rightPanel.activeTool) {
                derivedStateOf {
                    localRightRatio.takeIf {
                        state.workspace.shelf.rightPanel.activeTool != null
                    } ?: 0f
                }
            }

            val centerWeight by remember(leftWeight, rightWeight) {
                derivedStateOf {
                    (1f - leftWeight - rightWeight).coerceAtLeast(.2f)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShelfPanelContent(panel = state.workspace.shelf.leftPanel, selectTool = { tool ->
                        scope.launch {
                            execute(WorkspaceCommand.SelectShelfTool(tool = tool))
                        }
                    })

                    BoxWithConstraints(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val totalWidth = constraints.maxWidth.toFloat()

                        VerticalSplitPane(
                            modifier = Modifier.fillMaxSize(), splitPaneState = verticalSplitPaneState
                        ) {
                            first {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (leftWeight > 0f) {
                                        Box(modifier = Modifier.weight(leftWeight)) {
                                            state.workspace.shelf.leftPanel.activeTool?.let { tool ->
                                                ShelfToolContent(tool = tool, explorer = {
                                                    explorer(state.workspace.activeDocument?.path)
                                                }, log = log)
                                            }
                                        }

                                        ShelfPanelContentHandle(
                                            totalWidth = totalWidth, onPositionChange = { deltaX ->
                                                if (totalWidth > 0f) {
                                                    val currentRatio = localLeftRatio

                                                    val delta = deltaX / totalWidth

                                                    localLeftRatio = (localLeftRatio + delta).coerceIn(.05f, .4f)

                                                    if (localLeftRatio != currentRatio) {
                                                        scope.launch {
                                                            leftRatioChannel.send(localLeftRatio)
                                                        }
                                                    }
                                                }
                                            })
                                    }

                                    Box(modifier = Modifier.weight(centerWeight), contentAlignment = Alignment.Center) {
                                        state.workspace.activeDocument?.let { activeDocument ->
                                            Container {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(
                                                        space = 4.dp, alignment = Alignment.CenterVertically
                                                    )
                                                ) {
                                                    CloseableTabs(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        items = state.workspace.documents,
                                                        activeItem = state.workspace.activeDocument,
                                                        getItemName = WorkspaceDocument::name,
                                                        select = { document ->
                                                            scope.launch {
                                                                execute(WorkspaceCommand.OpenDocument(document = document))
                                                            }
                                                        },
                                                        close = { document ->
                                                            scope.launch {
                                                                execute(WorkspaceCommand.CloseDocument(document = document))
                                                            }
                                                        })
                                                    Box(
                                                        modifier = Modifier.weight(1f),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        editor(activeDocument.path)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (rightWeight > 0f) {
                                        ShelfPanelContentHandle(
                                            totalWidth = totalWidth, onPositionChange = { deltaX ->
                                                if (totalWidth > 0f) {
                                                    val currentRatio = localRightRatio

                                                    val delta = -deltaX / totalWidth

                                                    localRightRatio = (localRightRatio + delta).coerceIn(.05f, .4f)

                                                    if (localRightRatio != currentRatio) {
                                                        scope.launch {
                                                            rightRatioChannel.send(localRightRatio)
                                                        }
                                                    }
                                                }
                                            })

                                        Box(modifier = Modifier.weight(rightWeight)) {
                                            state.workspace.shelf.rightPanel.activeTool?.let { tool ->
                                                ShelfToolContent(tool = tool, explorer = {
                                                    explorer(state.workspace.activeDocument?.path)
                                                }, log = log)
                                            }
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

                            second(minSize = 48.dp) {
                                output()
                            }
                        }
                    }

                    ShelfPanelContent(panel = state.workspace.shelf.rightPanel, selectTool = { tool ->
                        scope.launch {
                            execute(WorkspaceCommand.SelectShelfTool(tool = tool))
                        }
                    })
                }

                Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
                    status()
                }
            }
        })
}