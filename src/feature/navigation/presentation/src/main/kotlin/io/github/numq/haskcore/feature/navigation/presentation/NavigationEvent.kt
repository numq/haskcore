package io.github.numq.haskcore.feature.navigation.presentation

internal sealed interface NavigationEvent {
    data class HandleFailure(val throwable: Throwable) : NavigationEvent
}