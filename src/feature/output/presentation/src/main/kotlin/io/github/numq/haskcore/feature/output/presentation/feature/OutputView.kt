package io.github.numq.haskcore.feature.output.presentation.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.numq.haskcore.common.presentation.container.Container
import io.github.numq.haskcore.common.presentation.tab.CloseableTabs
import io.github.numq.haskcore.feature.output.core.OutputSession
import io.github.numq.haskcore.feature.output.presentation.session.OutputSessionItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun OutputView(projectScope: Scope, handleError: (Throwable) -> Unit) {
    val scope = rememberCoroutineScope()

    val feature = koinInject<OutputFeature>(scope = projectScope)

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
                        modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface
                    )

                    OutputSessionItem(
                        modifier = Modifier.weight(1f),
                        session = activeSession,
                        menu = state.menu,
                        openMenu = { (x, y) ->
                            scope.launch {
                                feature.execute(OutputCommand.OpenMenu(x = x, y = y))
                            }
                        },
                        closeMenu = {
                            scope.launch {
                                feature.execute(OutputCommand.CloseMenu)
                            }
                        },
                        copyText = {
                            scope.launch {
                                feature.execute(OutputCommand.CopyText(session = activeSession))
                            }
                        })
                }
            }
        }
    }
}