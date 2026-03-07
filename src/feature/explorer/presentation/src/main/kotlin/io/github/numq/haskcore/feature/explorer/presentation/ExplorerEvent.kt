package io.github.numq.haskcore.feature.explorer.presentation

internal sealed interface ExplorerEvent {
    data class HandleFailure(val throwable: Throwable) : ExplorerEvent
}