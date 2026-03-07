package io.github.numq.haskcore.feature.explorer.presentation

import io.github.numq.haskcore.feature.explorer.core.ExplorerPosition
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import kotlinx.coroutines.flow.Flow

internal sealed interface ExplorerCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, TOGGLE_EXPLORER_NODE, SELECT_EXPLORER_NODE, SAVE_EXPLORER_POSITION, OPEN_FILE
    }

    data class HandleFailure(val throwable: Throwable) : ExplorerCommand

    data object Initialize : ExplorerCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<ExplorerTree>) : ExplorerCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateExplorerTree(val explorerTree: ExplorerTree) : ExplorerCommand

    data class ToggleExplorerNode(val path: String) : ExplorerCommand {
        val key = Key.TOGGLE_EXPLORER_NODE
    }

    data object ToggleExplorerNodeSuccess : ExplorerCommand

    data class SelectExplorerNode(val path: String) : ExplorerCommand {
        val key = Key.SELECT_EXPLORER_NODE
    }

    data object SelectExplorerNodeSuccess : ExplorerCommand

    data class SaveExplorerPosition(val position: ExplorerPosition) : ExplorerCommand {
        val key = Key.SAVE_EXPLORER_POSITION
    }

    data object SaveExplorerPositionSuccess : ExplorerCommand

    data class OpenFile(val path: String) : ExplorerCommand {
        val key = Key.OPEN_FILE
    }
}