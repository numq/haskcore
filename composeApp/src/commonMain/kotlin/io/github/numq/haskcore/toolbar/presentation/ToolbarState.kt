package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.workspace.Workspace

internal data class ToolbarState(
    val activeWorkspace: Workspace?,
    val recentWorkspaces: List<Workspace>,
    val workspaceMenuExpanded: Boolean = false,
)