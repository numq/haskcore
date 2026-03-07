package io.github.numq.haskcore.feature.log.presentation

internal sealed interface LogEvent {
    data class HandleFailure(val throwable: Throwable) : LogEvent
}