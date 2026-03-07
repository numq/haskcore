package io.github.numq.haskcore.feature.welcome.presentation

import io.github.numq.haskcore.feature.welcome.core.RecentProject
import kotlinx.coroutines.flow.Flow

internal sealed interface WelcomeCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, REMOVE_RECENT_PROJECT
    }

    data class HandleFailure(val throwable: Throwable) : WelcomeCommand

    data object Initialize : WelcomeCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<List<RecentProject>>) : WelcomeCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateRecentProjects(val recentProjects: List<RecentProject>) : WelcomeCommand

    data class OpenProject(val path: String, val name: String?) : WelcomeCommand

    data class RemoveRecentProject(val path: String) : WelcomeCommand {
        val key = Key.REMOVE_RECENT_PROJECT
    }

    data object RemoveRecentProjectSuccess : WelcomeCommand

    data object ExitApplication : WelcomeCommand
}