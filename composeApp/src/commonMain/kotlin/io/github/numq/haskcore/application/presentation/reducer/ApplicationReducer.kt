package io.github.numq.haskcore.application.presentation.reducer

import io.github.numq.haskcore.application.presentation.ApplicationCommand
import io.github.numq.haskcore.application.presentation.ApplicationState
import io.github.numq.haskcore.feature.Event
import io.github.numq.haskcore.feature.Reducer
import io.github.numq.haskcore.session.usecase.GetSession
import io.github.numq.haskcore.workspace.usecase.OpenWorkspace
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

internal class ApplicationReducer(
    private val getSession: GetSession, private val openWorkspace: OpenWorkspace
) : Reducer<ApplicationCommand, ApplicationState> {
    override suspend fun reduce(state: ApplicationState, command: ApplicationCommand) = when (command) {
        is ApplicationCommand.Initialize -> coroutineScope {
            val openRecentWorkspace = async {
                getSession.execute(input = Unit).mapCatching { session ->
                    when (val path = session.workspacePath) {
                        null -> Unit

                        else -> openWorkspace.execute(input = OpenWorkspace.Input(path = path)).getOrThrow()
                    }
                }.fold(onSuccess = { session ->
                    transition(state)
                }) { throwable ->
                    transition(state, Event.Failure(throwable = throwable))
                }
            }

            delay(1.seconds)

            openRecentWorkspace.await()
        }

        is ApplicationCommand.ChangeDividerPosition -> when (state) {
            is ApplicationState.Splash -> transition(state)

            is ApplicationState.Content -> {
                // todo save

                transition(state.copy(dividerPercentage = command.percentage))
            }
        }

        is ApplicationCommand.OpenDialog -> when (state) {
            is ApplicationState.Splash -> transition(state)

            is ApplicationState.Content -> transition(state.copy(dialog = command.dialog))
        }

        is ApplicationCommand.CloseDialog -> when (state) {
            is ApplicationState.Splash -> transition(state)

            is ApplicationState.Content -> transition(state.copy(dialog = null))
        }
    }
}