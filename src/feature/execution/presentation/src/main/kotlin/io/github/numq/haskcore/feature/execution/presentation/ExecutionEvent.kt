package io.github.numq.haskcore.feature.execution.presentation

internal sealed interface ExecutionEvent {
    data class HandleFailure(val throwable: Throwable) : ExecutionEvent
}