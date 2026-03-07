package io.github.numq.haskcore.feature.status.presentation

internal sealed interface StatusEvent {
    data class HandleFailure(val throwable: Throwable) : StatusEvent
}