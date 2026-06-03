package io.github.numq.haskcore.feature.output.presentation.feature

sealed interface OutputEvent {
    data class HandleFailure(val throwable: Throwable) : OutputEvent
}