package io.github.numq.haskcore.feature.output.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.overlay.menu.ContextMenu
import io.github.numq.haskcore.common.presentation.overlay.menu.ContextMenuItem
import io.github.numq.haskcore.common.presentation.tab.CloseableTabs
import io.github.numq.haskcore.feature.output.core.OutputSession
import io.github.numq.haskcore.feature.output.presentation.menu.ContextMenuState
import io.github.numq.haskcore.feature.output.presentation.session.OutputSessionItem
import kotlinx.coroutines.launch

@Composable
fun OutputView(feature: OutputFeature, handleError: (Throwable) -> Unit) {
    val scope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is OutputEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    state.output.activeSession?.let { activeSession ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Container {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    CloseableTabs(
                        modifier = Modifier.fillMaxWidth(),
                        items = state.output.sessions,
                        activeItem = activeSession,
                        getItemName = OutputSession::name,
                        select = { session ->
                            scope.launch {
                                feature.execute(OutputCommand.SelectSession(sessionId = session.id))
                            }
                        },
                        close = { session ->
                            scope.launch {
                                feature.execute(OutputCommand.CloseSession(sessionId = session.id))
                            }
                        })

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    )

                    ContextMenu(
                        offset = when (val menuState = state.contextMenuState) {
                            is ContextMenuState.Hidden -> Offset.Unspecified

                            is ContextMenuState.Visible -> Offset(
                                x = menuState.x, y = menuState.y
                            )
                        }, onOpen = { (x, y) ->
                            scope.launch {
                                feature.execute(OutputCommand.OpenMenu(x = x, y = y))
                            }
                        }, onClose = {
                            scope.launch {
                                feature.execute(OutputCommand.CloseMenu)
                            }
                        }, items = {
                            listOf(
                                ContextMenuItem(
                                    label = "Copy text",
                                    leadingIcon = Icons.Default.ContentCopy,
                                    enabled = activeSession.lines.isNotEmpty(),
                                    onClick = {
                                        scope.launch {
                                            feature.execute(OutputCommand.CopyText(session = activeSession))
                                        }
                                    })
                            )
                        }) {
                        OutputSessionItem(
                            modifier = Modifier.weight(1f), session = activeSession
                        )
                    }
                }
            }
        }
    }
}