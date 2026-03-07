package io.github.numq.haskcore.feature.navigation.presentation

import io.github.numq.haskcore.feature.navigation.core.Destination
import kotlinx.coroutines.flow.Flow

internal sealed interface NavigationCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, OPEN_PROJECT
    }

    data class HandleFailure(val throwable: Throwable) : NavigationCommand

    data object Initialize : NavigationCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<List<Destination>>) : NavigationCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateDestinations(val destinations: List<Destination>) : NavigationCommand

    data class OpenProject(val path: String, val name: String?) : NavigationCommand {
        val key = Key.OPEN_PROJECT
    }

    data object OpenProjectSuccess : NavigationCommand
}