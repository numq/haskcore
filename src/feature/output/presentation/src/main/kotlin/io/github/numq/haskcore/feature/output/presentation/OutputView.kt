package io.github.numq.haskcore.feature.output.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun OutputView(projectScope: Scope, handleError: (Throwable) -> Unit) {
    if (projectScope.closed) return

    val feature = koinInject<OutputFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is OutputEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        OutputTabs(
            sessions = state.output.sessions,
            selectedSession = state.output.selectedSession,
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

        state.output.selectedSession?.let { selectedSession ->
            OutputSessionItem(session = selectedSession)
        }
    }
}