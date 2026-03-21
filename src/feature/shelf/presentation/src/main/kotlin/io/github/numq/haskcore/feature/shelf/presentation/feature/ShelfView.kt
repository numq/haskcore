package io.github.numq.haskcore.feature.shelf.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.numq.haskcore.feature.shelf.presentation.panel.ShelfPanelContent
import io.github.numq.haskcore.feature.shelf.presentation.tool.ShelfToolContent
import io.github.numq.haskcore.platform.splitpane.TripleSplitPane
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun ShelfView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    explorer: @Composable () -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable () -> Unit,
) {
    val feature = koinInject<ShelfFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is ShelfEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val scope = rememberCoroutineScope()

    val leftRatioChannel = remember { Channel<Float>(Channel.CONFLATED) }

    DisposableEffect(leftRatioChannel) {
        onDispose {
            leftRatioChannel.close()
        }
    }

    LaunchedEffect(leftRatioChannel) {
        leftRatioChannel.consumeAsFlow().debounce(500.milliseconds).collect { ratio ->
            feature.execute(ShelfCommand.SaveLeftRatio(ratio = ratio))
        }
    }

    val rightRatioChannel = remember { Channel<Float>(Channel.CONFLATED) }

    DisposableEffect(rightRatioChannel) {
        onDispose {
            rightRatioChannel.close()
        }
    }

    LaunchedEffect(rightRatioChannel) {
        rightRatioChannel.consumeAsFlow().debounce(500.milliseconds).collect { ratio ->
            feature.execute(ShelfCommand.SaveRightRatio(ratio = ratio))
        }
    }

    var localLeftRatio by remember(state.shelf.leftPanel.ratio) {
        mutableFloatStateOf(state.shelf.leftPanel.ratio)
    }

    var localRightRatio by remember(state.shelf.rightPanel.ratio) {
        mutableFloatStateOf(state.shelf.rightPanel.ratio)
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShelfPanelContent(panel = state.shelf.leftPanel, selectTool = { tool ->
            scope.launch {
                feature.execute(ShelfCommand.SelectShelfTool(tool = tool))
            }
        })

        TripleSplitPane(
            modifier = Modifier.weight(1f), leftWeight = localLeftRatio.takeIf {
                state.shelf.leftPanel.activeTool != null
            } ?: 0f, rightWeight = localRightRatio.takeIf {
                state.shelf.rightPanel.activeTool != null
            } ?: 0f, onLeftResize = { delta ->
                val currentRatio = localLeftRatio

                localLeftRatio = (localLeftRatio + delta).coerceIn(.05f, .4f)

                if (localLeftRatio != currentRatio) {
                    scope.launch {
                        leftRatioChannel.send(localLeftRatio)
                    }
                }
            }, onRightResize = { delta ->
                val currentRatio = localRightRatio

                localRightRatio = (localRightRatio + delta).coerceIn(.05f, .4f)

                if (localRightRatio != currentRatio) {
                    scope.launch {
                        rightRatioChannel.send(localRightRatio)
                    }
                }
            }, leftContent = {
                state.shelf.leftPanel.activeTool?.let { tool ->
                    ShelfToolContent(tool = tool, explorer = explorer, log = log)
                }
            }, rightContent = {
                state.shelf.rightPanel.activeTool?.let { tool ->
                    ShelfToolContent(tool = tool, explorer = explorer, log = log)
                }
            }, centerContent = editor
        )

        ShelfPanelContent(panel = state.shelf.rightPanel, selectTool = { tool ->
            scope.launch {
                feature.execute(ShelfCommand.SelectShelfTool(tool = tool))
            }
        })
    }
}