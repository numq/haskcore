package io.github.numq.haskcore.feature.workspace.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import io.github.numq.haskcore.common.core.language.Language
import io.github.numq.haskcore.feature.workspace.presentation.feature.view.WorkspaceViewLoading
import io.github.numq.haskcore.feature.workspace.presentation.feature.view.WorkspaceViewReady
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun WorkspaceView(
    projectScope: Scope,
    handleError: (Throwable) -> Unit,
    logo: Painter,
    exitApplication: () -> Unit,
    execution: @Composable () -> Unit,
    explorer: @Composable (path: String?) -> Unit,
    log: @Composable () -> Unit,
    editor: @Composable (path: String?, language: Language?) -> Unit,
    output: (@Composable () -> Unit)?,
    status: @Composable () -> Unit,
) {
    val feature = koinInject<WorkspaceFeature>(scope = projectScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is WorkspaceEvent.HandleFailure -> handleError(event.throwable)

                is WorkspaceEvent.ExitApplication -> exitApplication()
            }
        }
    }

    when (val currentState = state) {
        is WorkspaceState.Loading -> WorkspaceViewLoading()

        is WorkspaceState.Ready -> WorkspaceViewReady(
            state = currentState,
            execute = feature::execute,
            icon = logo,
            execution = execution,
            explorer = explorer,
            log = log,
            editor = editor,
            output = output,
            status = status
        )
    }
}