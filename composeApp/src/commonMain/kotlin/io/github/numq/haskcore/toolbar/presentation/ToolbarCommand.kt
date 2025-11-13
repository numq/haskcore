package io.github.numq.haskcore.toolbar.presentation

import io.github.numq.haskcore.session.RecentWorkspace
import io.github.numq.haskcore.toolbar.presentation.menu.ToolbarMenu
import io.github.numq.haskcore.workspace.Workspace

internal sealed interface ToolbarCommand {
    data object Initialize : ToolbarCommand

    data class UpdateRecentWorkspaces(val recentWorkspaces: List<RecentWorkspace>) : ToolbarCommand

    data class UpdateWorkspace(val workspace: Workspace) : ToolbarCommand

    data class OpenWorkspace(val path: String) : ToolbarCommand

    data object CloseWorkspace : ToolbarCommand

    data class OpenMenu(val menu: ToolbarMenu) : ToolbarCommand

    data object CloseMenu : ToolbarCommand

    data object MinimizeWindow : ToolbarCommand

    data object ToggleFullscreen : ToolbarCommand

    data object ExitApplication : ToolbarCommand
}