package io.github.numq.haskcore.feature.welcome.presentation.feature

internal sealed interface WelcomeEvent {
    data class HandleFailure(val throwable: Throwable) : WelcomeEvent

    data class OpenProject(val path: String, val name: String?) : WelcomeEvent

    data object ExitApplication : WelcomeEvent
}