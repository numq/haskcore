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
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.common.presentation.window.WindowDecoration
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceCommand
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceState
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContent
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
    explorer: @Composable () -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?, language: Language?) -> Unit,
    output: (@Composable () -> Unit)?,
    status: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val windowState = rememberWindowState(
        placement = when {
            state.workspace.isFullscreen -> WindowPlacement.Maximized

            else -> WindowPlacement.Floating
        },
        position = WindowPosition.Absolute(x = state.workspace.x.dp, y = state.workspace.y.dp),
        size = DpSize(width = state.workspace.width.dp, height = state.workspace.height.dp)
    )

    DisposableEffect(Unit) {
        onDispose {
            scope.launch(NonCancellable) {
                val isFullscreen = windowState.placement == WindowPlacement.Maximized

                execute(
                    WorkspaceCommand.SaveDimensions(
                        x = when {
                            isFullscreen -> state.workspace.x

                            else -> windowState.position.x.value
                        }, y = when {
                            isFullscreen -> state.workspace.y

                            else -> windowState.position.y.value
                        }, width = when {
                            isFullscreen -> state.workspace.width

                            else -> windowState.size.width.value
                        }, height = when {
                            isFullscreen -> state.workspace.height

                            else -> windowState.size.height.value
                        }, isFullscreen = isFullscreen
                    )
                )
            }
        }
    }

    LaunchedEffect(
        state.workspace.x,
        state.workspace.y,
        state.workspace.width,
        state.workspace.height,
        state.workspace.isFullscreen
    ) {
        when {
            state.workspace.isFullscreen -> windowState.placement = WindowPlacement.Maximized

            else -> {
                windowState.position = WindowPosition(state.workspace.x.dp, state.workspace.y.dp)

                windowState.size = DpSize(state.workspace.width.dp, state.workspace.height.dp)
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

            var lastActiveVerticalRatio by remember { mutableFloatStateOf(state.workspace.verticalRatio) }

            DisposableEffect(output) {
                onDispose {
                    if (output != null) {
                        val ratio = verticalSplitPaneState.positionPercentage

                        if (ratio != state.workspace.verticalRatio) {
                            scope.launch(NonCancellable) {
                                execute(WorkspaceCommand.SaveVerticalRatio(ratio = verticalSplitPaneState.positionPercentage))
                            }
                        }
                    }
                }
            }

            LaunchedEffect(output) {
                when (output) {
                    null -> verticalSplitPaneState.positionPercentage = 1f

                    else -> {
                        verticalSplitPaneState.positionPercentage = lastActiveVerticalRatio

                        snapshotFlow {
                            verticalSplitPaneState.positionPercentage
                        }.distinctUntilChanged().conflate().debounce(500.milliseconds)
                            .filterNot(state.workspace.verticalRatio::equals).collect { ratio ->
                                execute(WorkspaceCommand.SaveVerticalRatio(ratio = ratio))
                            }
                    }
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
                    VerticalSplitPane(
                        modifier = Modifier.weight(1f), splitPaneState = verticalSplitPaneState
                    ) {
                        first(minSize = 48.dp) {
                            WorkspaceContent(
                                workspace = state.workspace,
                                execute = execute,
                                leftWeight = leftWeight,
                                localLeftRatio = localLeftRatio,
                                changeLocalLeftRatio = { ratio ->
                                    scope.launch {
                                        leftRatioChannel.send(ratio)
                                    }
                                },
                                rightWeight = rightWeight,
                                localRightRatio = localRightRatio,
                                changeLocalRightRatio = { ratio ->
                                    scope.launch {
                                        rightRatioChannel.send(ratio)
                                    }
                                },
                                centerWeight = centerWeight,
                                explorer = explorer,
                                log = log,
                                editor = editor
                            )
                        }
                        if (output != null) {
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
                        }
                        second(minSize = 48.dp) {
                            when (output) {
                                null -> Spacer(modifier = Modifier.height(0.dp))

                                else -> output()
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