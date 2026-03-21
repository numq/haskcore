package io.github.numq.haskcore.feature.workspace.presentation.feature

internal sealed interface WorkspaceEvent {
    data class HandleFailure(val throwable: Throwable) : WorkspaceEvent
}