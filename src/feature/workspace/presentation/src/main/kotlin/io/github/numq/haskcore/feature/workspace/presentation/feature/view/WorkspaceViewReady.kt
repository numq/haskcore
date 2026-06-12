package io.github.numq.haskcore.feature.workspace.presentation.feature.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceCommand
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceState
import io.github.numq.haskcore.feature.workspace.presentation.feature.window.ResizeDirection
import io.github.numq.haskcore.feature.workspace.presentation.feature.window.WindowButton
import io.github.numq.haskcore.feature.workspace.presentation.feature.window.resizeHandle
import io.github.numq.haskcore.feature.workspace.presentation.shelf.ShelfPanelContent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.*
import kotlin.time.Duration.Companion.milliseconds
import java.awt.Color as AwtColor

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
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val targetScreenBounds = remember(state.workspace.x, state.workspace.y) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()

        val screens = ge.screenDevices

        val savedX = state.workspace.x.toInt()

        val savedY = state.workspace.y.toInt()

        val targetDevice = screens.firstOrNull { device ->
            device.defaultConfiguration.bounds.contains(savedX, savedY)
        } ?: ge.defaultScreenDevice

        val config = targetDevice.defaultConfiguration

        val bounds = config.bounds

        val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)

        Rectangle(
            bounds.x + insets.left,
            bounds.y + insets.top,
            bounds.width - insets.left - insets.right,
            bounds.height - insets.top - insets.bottom
        )
    }

    val windowState = rememberWindowState(position = with(density) {
        when {
            state.workspace.isFullscreen -> WindowPosition(
                x = targetScreenBounds.x.toDp(), y = targetScreenBounds.y.toDp()
            )

            else -> WindowPosition(
                x = state.workspace.x.toDp(), y = state.workspace.y.toDp().coerceAtLeast(targetScreenBounds.y.toDp())
            )
        }
    }, size = with(density) {
        when {
            state.workspace.isFullscreen -> DpSize(
                width = targetScreenBounds.width.toDp(), height = targetScreenBounds.height.toDp()
            )

            else -> DpSize(
                width = state.workspace.width.toDp()
                    .coerceIn(state.workspace.minWidth.dp, (targetScreenBounds.width * .95f).toDp()),
                height = state.workspace.height.toDp()
                    .coerceIn(state.workspace.minHeight.dp, (targetScreenBounds.height * .95f).toDp())
            )
        }
    })

    Window(
        onCloseRequest = {
            scope.launch {
                with(density) {
                    execute(
                        WorkspaceCommand.Close(
                            windowX = windowState.position.x.toPx(),
                            windowY = windowState.position.y.toPx(),
                            windowWidth = windowState.size.width.toPx(),
                            windowHeight = windowState.size.height.toPx()
                        )
                    )
                }
            }
        },
        state = windowState,
        title = state.workspace.name ?: state.workspace.path,
        icon = icon,
        undecorated = true,
        transparent = true,
        resizable = false,
    ) {
        val graphicsConfiguration = window.graphicsConfiguration

        val bounds = remember(graphicsConfiguration) {
            graphicsConfiguration.bounds
        }

        val insets = remember(graphicsConfiguration) {
            Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)
        }

        val screenPosition = remember(bounds, insets) {
            with(density) {
                WindowPosition(x = (bounds.x + insets.left).toDp(), y = (bounds.y + insets.top).toDp())
            }
        }

        val screenSize = remember(bounds, insets) {
            with(density) {
                DpSize(
                    width = (bounds.width - insets.left - insets.right).toDp(),
                    height = (bounds.height - insets.top - insets.bottom).toDp()
                )
            }
        }

        val backgroundColor = MaterialTheme.colorScheme.surface

        LaunchedEffect(window, backgroundColor) {
            window.background = AwtColor(backgroundColor.toArgb())
        }

        LaunchedEffect(state.workspace.isFullscreen, screenSize) {
            if (screenSize.width == 0.dp || screenSize.height == 0.dp) return@LaunchedEffect

            with(density) {
                when {
                    state.workspace.isFullscreen -> {
                        windowState.position = screenPosition

                        windowState.size = screenSize
                    }

                    else -> {
                        windowState.position = WindowPosition(
                            x = state.workspace.x.toDp(), y = state.workspace.y.toDp().coerceAtLeast(screenPosition.y)
                        )

                        windowState.size = DpSize(
                            width = state.workspace.width.toDp()
                                .coerceIn(state.workspace.minWidth.dp, screenSize.width * .95f),
                            height = state.workspace.height.toDp()
                                .coerceIn(state.workspace.minHeight.dp, screenSize.height * .95f)
                        )
                    }
                }
            }
        }

        LaunchedEffect(windowState.position) {
            if (state.workspace.isFullscreen) return@LaunchedEffect

            snapshotFlow {
                windowState.position
            }.distinctUntilChanged().debounce(300.milliseconds).collect { position ->
                with(density) {
                    val safeY = position.y.toPx().coerceAtLeast(targetScreenBounds.y.toFloat())

                    execute(
                        WorkspaceCommand.SaveDimensions(
                            x = position.x.toPx(),
                            y = safeY,
                            width = windowState.size.width.toPx(),
                            height = windowState.size.height.toPx()
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().composed {
                    when {
                        state.workspace.isFullscreen -> background(color = backgroundColor)

                        else -> {
                            val shape = RoundedCornerShape(size = 6.dp)

                            clip(shape = shape).background(color = backgroundColor).border(
                                width = Dp.Hairline, color = MaterialTheme.colorScheme.outline, shape = shape
                            )
                        }
                    }
                }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scope.launch {
                                    execute(WorkspaceCommand.ToggleFullscreen)
                                }
                            })
                    }.composed {
                        when {
                            state.workspace.isFullscreen -> this

                            else -> pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitFirstDown()

                                        val startMouse = MouseInfo.getPointerInfo().location
                                        val startLocation = window.location.clone() as? Point ?: continue

                                        val currentLocation = startLocation.clone() as? Point ?: continue

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: continue

                                            if (!change.pressed) break

                                            val nowMouse = MouseInfo.getPointerInfo().location
                                            val dx = nowMouse.x - startMouse.x
                                            val dy = nowMouse.y - startMouse.y

                                            currentLocation.x = startLocation.x + dx
                                            currentLocation.y =
                                                (startLocation.y + dy).coerceAtLeast(targetScreenBounds.y)

                                            window.location = currentLocation

                                            with(density) {
                                                windowState.position = WindowPosition(
                                                    x = currentLocation.x.toDp(), y = currentLocation.y.toDp()
                                                )
                                            }

                                            change.consume()
                                        }

                                        scope.launch {
                                            execute(
                                                WorkspaceCommand.SaveDimensions(
                                                    x = currentLocation.x.toFloat(),
                                                    y = currentLocation.y.toFloat(),
                                                    width = windowState.size.width.toPx(),
                                                    height = windowState.size.height.toPx()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            content = {
                                Icon(
                                    painter = icon,
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.aspectRatio(1f).padding(8.dp)
                                )

                                val path = state.workspace.path

                                val name = state.workspace.name

                                when (name) {
                                    null -> Text(
                                        text = "C:\\Users\\User\\Documents\\HaskellProject", // path, todo
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    else -> {
                                        Text(
                                            text = name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = "($path)",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            })
                    }

                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        execution()

                        WindowButton(icon = Icons.Default.Minimize, click = {
                            window.extendedState = Frame.ICONIFIED
                        })

                        WindowButton(
                            icon = when {
                                state.workspace.isFullscreen -> Icons.Default.FullscreenExit

                                else -> Icons.Default.Fullscreen
                            }, click = {
                                scope.launch {
                                    execute(WorkspaceCommand.ToggleFullscreen)
                                }
                            })

                        WindowButton(icon = Icons.Default.Close, click = {
                            scope.launch {
                                with(density) {
                                    execute(
                                        WorkspaceCommand.Close(
                                            windowX = windowState.position.x.toPx(),
                                            windowY = windowState.position.y.toPx(),
                                            windowWidth = windowState.size.width.toPx(),
                                            windowHeight = windowState.size.height.toPx()
                                        )
                                    )
                                }
                            }
                        })
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) {
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
                                    scope.launch {
                                        withContext(NonCancellable) {
                                            execute(WorkspaceCommand.SaveVerticalRatio(ratio = verticalSplitPaneState.positionPercentage))
                                        }
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

                    val centerWeight = (1f - leftWeight - rightWeight).coerceAtLeast(.2f)

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
                                                modifier = Modifier.markAsHandle().height(8.dp).fillMaxWidth()
                                                    .pointerHoverIcon(
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
                }
            }

            if (!state.workspace.isFullscreen) {
                val thickness = 4.dp
                val cornerSize = 12.dp

                ResizeDirection.entries.forEach { direction ->
                    Box(
                        modifier = Modifier.composed {
                            when (direction) {
                                ResizeDirection.TOP, ResizeDirection.BOTTOM -> fillMaxWidth().height(thickness)

                                ResizeDirection.LEFT, ResizeDirection.RIGHT -> fillMaxHeight().width(thickness)

                                ResizeDirection.TOP_LEFT, ResizeDirection.TOP_RIGHT, ResizeDirection.BOTTOM_LEFT, ResizeDirection.BOTTOM_RIGHT -> size(
                                    cornerSize
                                )
                            }
                        }.align(direction.alignment).resizeHandle(
                            direction = direction,
                            window = window,
                            windowState = windowState,
                            density = density,
                            minWidth = state.workspace.minWidth.toInt(),
                            minHeight = state.workspace.minHeight.toInt(),
                            onResize = { x, y, width, height ->
                                scope.launch {
                                    execute(
                                        WorkspaceCommand.SaveDimensions(
                                            x = x, y = y, width = width, height = height
                                        )
                                    )
                                }
                            })
                    )
                }
            }
        }
    }
}