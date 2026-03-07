package io.github.numq.haskcore.feature.workspace.presentation

internal sealed interface WorkspaceEvent {
    data class HandleFailure(val throwable: Throwable) : WorkspaceEvent
}