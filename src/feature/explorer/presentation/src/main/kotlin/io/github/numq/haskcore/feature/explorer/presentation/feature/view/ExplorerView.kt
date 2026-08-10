package io.github.numq.haskcore.feature.explorer.presentation.feature.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerFeature
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun ExplorerView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    navigateToPathCallback: (suspend (path: String) -> Unit) -> Unit,
) {
    val feature = koinInject<ExplorerFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        navigateToPathCallback { path ->
            feature.execute(ExplorerCommand.OpenPath(path = path))
        }

        feature.events.collect { event ->
            when (event) {
                is ExplorerEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    when (val currentState = state) {
        is ExplorerState.Loading -> ExplorerViewLoading()

        is ExplorerState.Ready -> ExplorerViewReady(state = currentState, execute = feature::execute)
    }
}