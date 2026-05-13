package io.github.numq.haskcore.feature.workspace.presentation.feature

import io.github.numq.haskcore.feature.workspace.core.ShelfTool
import io.github.numq.haskcore.feature.workspace.core.Workspace
import kotlinx.coroutines.flow.Flow

internal sealed interface WorkspaceCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, CLOSE_WORKSPACE, OPEN_TAB, CLOSE_TAB, SELECT_SHELF_TOOL, SAVE_LEFT_SHELF_PANEL_RATIO, SAVE_RIGHT_SHELF_PANEL_RATIO, SAVE_VERTICAL_RATIO, SAVE_DIMENSIONS
    }

    data class HandleFailure(val throwable: Throwable) : WorkspaceCommand

    data object Initialize : WorkspaceCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<Workspace>) : WorkspaceCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateWorkspace(val workspace: Workspace) : WorkspaceCommand

    data object CloseWorkspace : WorkspaceCommand {
        val key = Key.CLOSE_WORKSPACE
    }

    data object CloseWorkspaceSuccess : WorkspaceCommand

    data class OpenTab(val path: String) : WorkspaceCommand {
        val key = Key.OPEN_TAB
    }

    data object OpenTabSuccess : WorkspaceCommand

    data class CloseTab(val path: String) : WorkspaceCommand {
        val key = Key.CLOSE_TAB
    }

    data object CloseTabSuccess : WorkspaceCommand

    data class SelectShelfTool(val tool: ShelfTool) : WorkspaceCommand {
        val key = Key.SELECT_SHELF_TOOL
    }

    data object SelectShelfToolSuccess : WorkspaceCommand

    data class SaveLeftShelfPanelRatio(val ratio: Float) : WorkspaceCommand {
        val key = Key.SAVE_LEFT_SHELF_PANEL_RATIO
    }

    data object SaveLeftShelfPanelRatioSuccess : WorkspaceCommand

    data class SaveRightShelfPanelRatio(val ratio: Float) : WorkspaceCommand {
        val key = Key.SAVE_RIGHT_SHELF_PANEL_RATIO
    }

    data object SaveRightShelfPanelRatioSuccess : WorkspaceCommand

    data class SaveVerticalRatio(val ratio: Float) : WorkspaceCommand {
        val key = Key.SAVE_VERTICAL_RATIO
    }

    data object SaveVerticalRatioSuccess : WorkspaceCommand

    data class SaveDimensions(
        val x: Float, val y: Float, val width: Float, val height: Float, val isFullscreen: Boolean,
    ) : WorkspaceCommand {
        val key = Key.SAVE_DIMENSIONS
    }

    data object SaveDimensionsSuccess : WorkspaceCommand
}