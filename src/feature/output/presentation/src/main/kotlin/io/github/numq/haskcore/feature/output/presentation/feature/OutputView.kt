package io.github.numq.haskcore.feature.output.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.numq.haskcore.feature.output.presentation.session.OutputSessionItem
import io.github.numq.haskcore.feature.output.presentation.tab.OutputTabs
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OutputTabs(sessions = state.output.sessions, activeSession = state.output.activeSession, select = { session ->
            scope.launch {
                feature.execute(OutputCommand.SelectSession(sessionId = session.id))
            }
        }, close = { session ->
            scope.launch {
                feature.execute(OutputCommand.CloseSession(sessionId = session.id))
            }
        })

        state.output.activeSession?.let { selectedSession ->
            OutputSessionItem(session = selectedSession, menu = state.menu, openMenu = { (x, y) ->
                scope.launch {
                    feature.execute(OutputCommand.OpenMenu(x = x, y = y))
                }
            }, closeMenu = {
                scope.launch {
                    feature.execute(OutputCommand.CloseMenu)
                }
            }, copyText = {
                scope.launch {
                    feature.execute(OutputCommand.CopyText(session = selectedSession))
                }
            })
        }
    }
}