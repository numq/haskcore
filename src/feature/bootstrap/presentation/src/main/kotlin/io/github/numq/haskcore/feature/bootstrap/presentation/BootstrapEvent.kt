package io.github.numq.haskcore.feature.bootstrap.presentation

internal sealed interface BootstrapEvent {
    data class HandleFailure(val throwable: Throwable) : BootstrapEvent

    data object ExitApplication : BootstrapEvent
}