package io.github.numq.haskcore.feature.navigation.presentation.feature

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.navigation.core.usecase.GetDestinations
import io.github.numq.haskcore.feature.navigation.core.usecase.OpenProject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map

internal class NavigationReducer(
    private val getDestinations: GetDestinations, private val openProject: OpenProject
) : Reducer<NavigationState, NavigationCommand, NavigationEvent> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reduce(state: NavigationState, command: NavigationCommand) = when (command) {
        is NavigationCommand.HandleFailure -> transition(state).event(NavigationEvent.HandleFailure(throwable = command.throwable))

        is NavigationCommand.Initialize -> transition(state).effect(
            action(key = command.key, fallback = NavigationCommand::HandleFailure, block = {
                getDestinations(input = Unit).fold(
                    ifLeft = NavigationCommand::HandleFailure, ifRight = NavigationCommand::InitializeSuccess
                )
            })
        )

        is NavigationCommand.InitializeSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(NavigationCommand::UpdateDestinations),
                fallback = NavigationCommand::HandleFailure
            )
        )

        is NavigationCommand.UpdateDestinations -> transition(NavigationState(destinations = command.destinations))

        is NavigationCommand.OpenProject -> transition(state).effect(
            action(
                key = command.key, fallback = NavigationCommand::HandleFailure, block = {
                    openProject(OpenProject.Input(path = command.path, name = command.name)).fold(
                        ifLeft = NavigationCommand::HandleFailure, ifRight = {
                            NavigationCommand.OpenProjectSuccess
                        })
                })
        )

        is NavigationCommand.OpenProjectSuccess -> transition(state)
    }
}