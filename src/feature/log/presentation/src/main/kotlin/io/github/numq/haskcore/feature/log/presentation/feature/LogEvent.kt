package io.github.numq.haskcore.feature.log.presentation.feature

internal sealed interface LogEvent {
    data class HandleFailure(val throwable: Throwable) : LogEvent
}