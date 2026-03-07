package io.github.numq.haskcore.feature.navigation.presentation

import androidx.compose.runtime.*
import io.github.numq.haskcore.feature.navigation.core.Destination
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

@Composable
fun NavigationView(
    applicationScope: Scope,
    handleError: (Throwable) -> Unit,
    initialDestinations: List<Destination>,
    welcome: @Composable (openProject: (path: String, name: String?) -> Unit) -> Unit,
    workspace: @Composable (Destination) -> Unit
) {
    if (applicationScope.closed) return

    val feature = koinInject<NavigationFeature>(scope = applicationScope) {
        parametersOf(initialDestinations)
    }

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is NavigationEvent.HandleFailure -> handleError(event.throwable)
            }
        }
    }

    val scope = rememberCoroutineScope()

    state.destinations.takeIf(List<Destination>::isNotEmpty)?.forEach { destination ->
        workspace(destination)
    } ?: welcome { path, name ->
        scope.launch {
            feature.execute(NavigationCommand.OpenProject(path = path, name = name))
        }
    }
}