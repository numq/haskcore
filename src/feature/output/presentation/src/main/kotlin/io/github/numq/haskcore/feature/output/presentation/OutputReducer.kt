package io.github.numq.haskcore.feature.output.presentation

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.output.core.usecase.CloseOutputSession
import io.github.numq.haskcore.feature.output.core.usecase.ObserveOutput
import io.github.numq.haskcore.feature.output.core.usecase.SelectOutputSession
import kotlinx.coroutines.flow.map

internal class OutputReducer(
    private val closeOutputSession: CloseOutputSession,
    private val observeOutput: ObserveOutput,
    private val selectOutputSession: SelectOutputSession
) : Reducer<OutputState, OutputCommand, OutputEvent> {
    override fun reduce(state: OutputState, command: OutputCommand) = when (command) {
        is OutputCommand.HandleFailure -> transition(state).event(OutputEvent.HandleFailure(throwable = command.throwable))

        is OutputCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = OutputCommand::HandleFailure, block = {
                    observeOutput(input = Unit).fold(
                        ifLeft = OutputCommand::HandleFailure, ifRight = OutputCommand::InitializeSuccess
                    )
                })
        )

        is OutputCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(OutputCommand::UpdateOutput),
                fallback = OutputCommand::HandleFailure
            )
        )

        is OutputCommand.UpdateOutput -> transition(state.copy(output = command.output))

        is OutputCommand.SelectSession -> transition(state).effect(
            action(
                key = command.key, fallback = OutputCommand::HandleFailure, block = {
                    selectOutputSession(input = SelectOutputSession.Input(sessionId = command.sessionId)).fold(
                        ifLeft = OutputCommand::HandleFailure, ifRight = { OutputCommand.SelectSessionSuccess })
                })
        )

        is OutputCommand.SelectSessionSuccess -> transition(state)

        is OutputCommand.CloseSession -> transition(state).effect(
            action(
                key = command.key, fallback = OutputCommand::HandleFailure, block = {
                    closeOutputSession(input = CloseOutputSession.Input(sessionId = command.sessionId)).fold(
                        ifLeft = OutputCommand::HandleFailure, ifRight = { OutputCommand.CloseSessionSuccess })
                })
        )

        is OutputCommand.CloseSessionSuccess -> transition(state)
    }
}