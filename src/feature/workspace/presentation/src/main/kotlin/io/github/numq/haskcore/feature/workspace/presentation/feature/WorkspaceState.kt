package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.feature.workspace.core.Workspace

internal sealed interface WorkspaceState {
    data object Loading : WorkspaceState

    data class Ready(val workspace: Workspace) : WorkspaceState
}