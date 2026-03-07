package io.github.numq.haskcore.feature.output.presentation

internal sealed interface OutputEvent {
    data class HandleFailure(val throwable: Throwable) : OutputEvent
}