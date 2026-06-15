package io.github.numq.haskcore.feature.log.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.log.core.usecase.ClearLogs
import io.github.numq.haskcore.feature.log.core.usecase.ObserveLogs
import kotlinx.coroutines.flow.map

internal class LogReducer(
    private val clearLogs: ClearLogs, private val observeLogs: ObserveLogs,
) : Reducer<LogState, LogCommand, LogEvent> {
    override fun reduce(state: LogState, command: LogCommand) = when (command) {
        is LogCommand.HandleFailure -> transition(state).event(LogEvent.HandleFailure(throwable = command.throwable))

        is LogCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = LogCommand::HandleFailure, block = {
                    observeLogs(input = Unit).fold(
                        ifLeft = LogCommand::HandleFailure, ifRight = LogCommand::InitializeSuccess
                    )
                })
        )

        is LogCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key, flow = command.flow.map(LogCommand::UpdateLogs), fallback = LogCommand::HandleFailure
            )
        )

        is LogCommand.UpdateLogs -> transition(state.copy(logs = command.logs))

        is LogCommand.Clear -> transition(state).effect(
            action(
                key = command.key, fallback = LogCommand::HandleFailure, block = {
                    clearLogs().fold(
                        ifLeft = LogCommand::HandleFailure, ifRight = { LogCommand.ClearSuccess })
                })
        )

        is LogCommand.ClearSuccess -> transition(state)
    }
}