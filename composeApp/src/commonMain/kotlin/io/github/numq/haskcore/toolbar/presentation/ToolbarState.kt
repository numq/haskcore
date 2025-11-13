package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.session.RecentWorkspace
import io.github.numq.haskcore.toolbar.presentation.menu.ToolbarMenu
import io.github.numq.haskcore.workspace.Workspace

internal data class ToolbarState(
    val activeWorkspace: Workspace = Workspace.None,
    val recentWorkspaces: List<RecentWorkspace> = emptyList(),
    val menu: ToolbarMenu? = null
)