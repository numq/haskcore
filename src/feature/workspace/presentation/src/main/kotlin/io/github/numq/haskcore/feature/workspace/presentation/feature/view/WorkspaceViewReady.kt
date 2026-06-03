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
    explorer: @Composable (path: String?) -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?, language: Language?) -> Unit,
    output: (@Composable () -> Unit)?,
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

    DisposableEffect(Unit) {
        onDispose {
            val workspace = state.workspace

            val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

            val x = workspace.x.takeIf { isFullscreen } ?: windowState.position.x.value

            val y = workspace.y.takeIf { isFullscreen } ?: windowState.position.y.value

            val width = workspace.width.takeIf { isFullscreen } ?: windowState.size.width.value

            val height = workspace.height.takeIf { isFullscreen } ?: windowState.size.height.value

            scope.launch(NonCancellable) {
                if (x != workspace.x || y != workspace.y || width != workspace.width || height != workspace.height || isFullscreen != workspace.isFullscreen) {
                    execute(
                        WorkspaceCommand.SaveDimensions(
                            x = x, y = y, width = width, height = height, isFullscreen = isFullscreen
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            Triple(windowState.position, windowState.size, windowState.placement)
        }.distinctUntilChanged().conflate().debounce(500.milliseconds).collect { (position, size, placement) ->
            val workspace = state.workspace

            val isFullscreen = placement == WindowPlacement.Fullscreen

            val placementChanged = isFullscreen != workspace.isFullscreen

            when {
                placementChanged -> {
                    val x = workspace.x.takeIf { isFullscreen } ?: position.x.value

                    val y = workspace.y.takeIf { isFullscreen } ?: position.y.value

                    val width = workspace.width.takeIf { isFullscreen } ?: size.width.value

                    val height = workspace.height.takeIf { isFullscreen } ?: size.height.value

                    execute(
                        WorkspaceCommand.SaveDimensions(
                            x = x, y = y, width = width, height = height, isFullscreen = isFullscreen
                        )
                    )
                }

                !isFullscreen -> {
                    val x = position.x.value

                    val y = position.y.value

                    val width = size.width.value

                    val height = size.height.value

                    if (x != workspace.x || y != workspace.y || width != workspace.width || height != workspace.height) {
                        execute(WorkspaceCommand.SaveDimensions(x, y, width, height, isFullscreen = false))
                    }
                }
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