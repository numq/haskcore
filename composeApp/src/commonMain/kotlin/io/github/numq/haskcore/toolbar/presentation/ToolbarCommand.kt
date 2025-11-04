package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.workspace.Workspace

internal sealed interface ToolbarCommand {
    data object Initialize : ToolbarCommand

    data class UpdateRecentWorkspaces(val recentWorkspaces: List<Workspace>) : ToolbarCommand

    data class UpdateWorkspace(val workspace: Workspace?) : ToolbarCommand

    data class OpenWorkspace(val path: String) : ToolbarCommand

    data object CloseWorkspace : ToolbarCommand

    data object ExpandWorkspaceMenu : ToolbarCommand

    data object CollapseWorkspaceMenu : ToolbarCommand

    data object MinimizeWindow : ToolbarCommand

    data object ToggleFullscreen : ToolbarCommand

    data object ExitApplication : ToolbarCommand
}