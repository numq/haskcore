package io.github.numq.haskcore.feature.stack.presentation.feature

internal sealed interface StackEvent {
    data class HandleFailure(val throwable: Throwable) : StackEvent
}