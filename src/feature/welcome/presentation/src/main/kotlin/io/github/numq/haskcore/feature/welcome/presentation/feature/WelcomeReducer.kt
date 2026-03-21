package io.github.numq.haskcore.feature.welcome.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.welcome.core.usecase.ObserveRecentProjects
import io.github.numq.haskcore.feature.welcome.core.usecase.RemoveRecentProject
import kotlinx.coroutines.flow.map

internal class WelcomeReducer(
    private val observeRecentProjects: ObserveRecentProjects, private val removeRecentProject: RemoveRecentProject
) : Reducer<WelcomeState, WelcomeCommand, WelcomeEvent> {
    override fun reduce(state: WelcomeState, command: WelcomeCommand) = when (command) {
        is WelcomeCommand.HandleFailure -> transition(state).event(WelcomeEvent.HandleFailure(throwable = command.throwable))

        is WelcomeCommand.Initialize -> transition(state).effect(
            action(
                key = command.key, fallback = WelcomeCommand::HandleFailure, block = {
                    observeRecentProjects(input = Unit).fold(
                        ifLeft = WelcomeCommand::HandleFailure, ifRight = WelcomeCommand::InitializeSuccess
                    )
                })
        )

        is WelcomeCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(WelcomeCommand::UpdateRecentProjects),
                fallback = WelcomeCommand::HandleFailure
            )
        )

        is WelcomeCommand.UpdateRecentProjects -> transition(state.copy(recentProjects = command.recentProjects))

        is WelcomeCommand.OpenProject -> transition(state).event(
            WelcomeEvent.OpenProject(path = command.path, name = command.name)
        )

        is WelcomeCommand.RemoveRecentProject -> transition(state).effect(
            action(
                key = command.key, fallback = WelcomeCommand::HandleFailure, block = {
                    removeRecentProject(input = RemoveRecentProject.Input(path = command.path)).fold(
                        ifLeft = WelcomeCommand::HandleFailure, ifRight = { WelcomeCommand.RemoveRecentProjectSuccess })
                })
        )

        is WelcomeCommand.RemoveRecentProjectSuccess -> transition(state)

        is WelcomeCommand.ExitApplication -> transition(state).event(WelcomeEvent.ExitApplication)
    }
}