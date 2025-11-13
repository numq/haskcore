package io.github.numq.haskcore.toolbar.presentation.menu

internal sealed interface ToolbarMenu {
    sealed interface Workspace : ToolbarMenu {
        data object Root : Workspace

        data object RecentWorkspaces : Workspace
    }
}