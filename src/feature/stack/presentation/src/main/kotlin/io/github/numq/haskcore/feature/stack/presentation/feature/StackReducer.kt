package io.github.numq.haskcore.feature.stack.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.stack.core.usecase.ClearLogs
import io.github.numq.haskcore.feature.stack.core.usecase.ObserveLogs
import kotlinx.coroutines.flow.map

internal class StackReducer(
    private val clearLogs: ClearLogs, private val observeLogs: ObserveLogs,
) : Reducer<StackState, StackCommand, StackEvent> {
    override fun reduce(state: StackState, command: StackCommand) = when (command) {
        is StackCommand.HandleFailure -> transition(state).event(StackEvent.HandleFailure(throwable = command.throwable))

        is StackCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = StackCommand::HandleFailure, block = {
                    observeLogs(input = Unit).fold(
                        ifLeft = StackCommand::HandleFailure, ifRight = StackCommand::InitializeSuccess
                    )
                })
        )

        is StackCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(StackCommand::UpdateLogs),
                fallback = StackCommand::HandleFailure
            )
        )

        is StackCommand.UpdateLogs -> transition(state.copy(logs = command.logs))

        is StackCommand.Clear -> transition(state).effect(
            action(
                key = command.key, fallback = StackCommand::HandleFailure, block = {
                    clearLogs().fold(
                        ifLeft = StackCommand::HandleFailure, ifRight = { StackCommand.ClearSuccess })
                })
        )

        is StackCommand.ClearSuccess -> transition(state)
    }
}