package io.github.numq.haskcore.feature.bootstrap.presentation

import io.github.numq.haskcore.core.feature.Reducer
import io.github.numq.haskcore.core.feature.action
import io.github.numq.haskcore.core.feature.effect
import io.github.numq.haskcore.core.feature.event
import io.github.numq.haskcore.feature.bootstrap.core.usecase.Boot

internal class BootstrapReducer(
    private val boot: Boot
) : Reducer<BootstrapState, BootstrapCommand, BootstrapEvent> {
    override fun reduce(state: BootstrapState, command: BootstrapCommand) = when (command) {
        is BootstrapCommand.HandleFailure -> transition(state).event(BootstrapEvent.HandleFailure(throwable = command.throwable))

        is BootstrapCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = BootstrapCommand::HandleFailure, block = {
                    boot(input = Unit).fold(
                        ifLeft = BootstrapCommand::HandleFailure, ifRight = BootstrapCommand::UpdateBootstrap
                    )
                })
        )

        is BootstrapCommand.UpdateBootstrap -> transition(BootstrapState.Content(bootstrap = command.bootstrap))
    }
}