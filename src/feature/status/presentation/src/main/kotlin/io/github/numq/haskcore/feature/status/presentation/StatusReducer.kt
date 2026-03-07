package io.github.numq.haskcore.feature.status.presentation

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.status.core.usecase.ObserveStatus
import io.github.numq.haskcore.feature.status.core.usecase.UpdatePaths
import kotlinx.coroutines.flow.map

internal class StatusReducer(
    private val observeStatus: ObserveStatus, private val updatePaths: UpdatePaths
) : Reducer<StatusState, StatusCommand, StatusEvent> {
    override fun reduce(state: StatusState, command: StatusCommand) = when (command) {
        is StatusCommand.HandleFailure -> transition(state).event(StatusEvent.HandleFailure(throwable = command.throwable))

        is StatusCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    observeStatus(input = Unit).fold(
                        ifLeft = StatusCommand::HandleFailure, ifRight = StatusCommand::InitializeSuccess
                    )
                })
        )

        is StatusCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(StatusCommand::UpdateStatus),
                fallback = StatusCommand::HandleFailure
            )
        )

        is StatusCommand.UpdateStatus -> transition(state.copy(status = command.status))

        is StatusCommand.ResetGhcPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = "", cabalPath = null, stackPath = null, hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.ResetGhcPathSuccess })
                })
        )

        is StatusCommand.ResetGhcPathSuccess -> transition(state)

        is StatusCommand.ResetCabalPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = "", stackPath = null, hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.ResetCabalPathSuccess })
                })
        )

        is StatusCommand.ResetCabalPathSuccess -> transition(state)

        is StatusCommand.ResetStackPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = null, stackPath = "", hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.ResetStackPathSuccess })
                })
        )

        is StatusCommand.ResetStackPathSuccess -> transition(state)

        is StatusCommand.ResetHlsPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = null, stackPath = null, hlsPath = ""
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.ResetHlsPathSuccess })
                })
        )

        is StatusCommand.ResetHlsPathSuccess -> transition(state)

        is StatusCommand.SelectGhcPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = command.path, cabalPath = null, stackPath = null, hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.SelectGhcPathSuccess })
                })
        )

        is StatusCommand.SelectGhcPathSuccess -> transition(state)

        is StatusCommand.SelectCabalPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = command.path, stackPath = null, hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.SelectCabalPathSuccess })
                })
        )

        is StatusCommand.SelectCabalPathSuccess -> transition(state)

        is StatusCommand.SelectStackPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = null, stackPath = command.path, hlsPath = null
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.SelectStackPathSuccess })
                })
        )

        is StatusCommand.SelectStackPathSuccess -> transition(state)

        is StatusCommand.SelectHlsPath -> transition(state).effect(
            action(
                key = command.key, fallback = StatusCommand::HandleFailure, block = {
                    updatePaths(
                        input = UpdatePaths.Input(
                            ghcPath = null, cabalPath = null, stackPath = null, hlsPath = command.path
                        )
                    ).fold(ifLeft = StatusCommand::HandleFailure, ifRight = { StatusCommand.SelectHlsPathSuccess })
                })
        )

        is StatusCommand.SelectHlsPathSuccess -> transition(state)
    }
}