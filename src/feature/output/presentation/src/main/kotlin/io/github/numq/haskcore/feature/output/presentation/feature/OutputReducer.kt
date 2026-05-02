package io.github.numq.haskcore.feature.output.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.Reducer
import io.github.numq.haskcore.common.presentation.feature.action
import io.github.numq.haskcore.common.presentation.feature.effect
import io.github.numq.haskcore.common.presentation.feature.event
import io.github.numq.haskcore.common.presentation.feature.stream
import io.github.numq.haskcore.feature.output.core.usecase.CloseOutputSession
import io.github.numq.haskcore.feature.output.core.usecase.CopySessionText
import io.github.numq.haskcore.feature.output.core.usecase.ObserveOutput
import io.github.numq.haskcore.feature.output.core.usecase.OpenOutputSession
import io.github.numq.haskcore.feature.output.presentation.menu.OutputMenu
import kotlinx.coroutines.flow.map

internal class OutputReducer(
    private val closeOutputSession: CloseOutputSession,
    private val copySessionText: CopySessionText,
    private val observeOutput: ObserveOutput,
    private val openOutputSession: OpenOutputSession,
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
                    openOutputSession(input = OpenOutputSession.Input(sessionId = command.sessionId)).fold(
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

        is OutputCommand.OpenMenu -> with(command) {
            when (state.output.activeSession) {
                null -> transition(state)

                else -> transition(state.copy(menu = OutputMenu.Visible(x = x, y = y)))
            }
        }

        is OutputCommand.CloseMenu -> with(command) {
            when (state.menu) {
                is OutputMenu.Hidden -> transition(state)

                is OutputMenu.Visible -> transition(state.copy(menu = OutputMenu.Hidden))
            }
        }

        is OutputCommand.CopyText -> transition(state).effect(
            action(
                key = command.key, fallback = OutputCommand::HandleFailure, block = {
                    copySessionText(input = CopySessionText.Input(session = command.session)).fold(
                        ifLeft = OutputCommand::HandleFailure, ifRight = { OutputCommand.CopyTextSuccess })
                })
        )

        is OutputCommand.CopyTextSuccess -> transition(state)
    }
}