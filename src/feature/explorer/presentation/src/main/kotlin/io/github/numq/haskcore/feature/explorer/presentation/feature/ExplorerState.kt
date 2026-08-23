package io.github.numq.haskcore.feature.explorer.presentation.feature

import io.github.numq.haskcore.feature.explorer.core.ExplorerNode
import io.github.numq.haskcore.feature.explorer.core.ExplorerTree
import io.github.numq.haskcore.feature.explorer.presentation.dialog.ExplorerDialog
import io.github.numq.haskcore.feature.explorer.presentation.menu.ContextMenuState

internal sealed interface ExplorerState {
    data object Loading : ExplorerState

    data class Ready(
        val explorerTree: ExplorerTree,
        val selectedNodes: List<ExplorerNode> = emptyList(),
        val contextMenuState: ContextMenuState = ContextMenuState.Hidden,
        val dialog: ExplorerDialog = ExplorerDialog.None,
    ) : ExplorerState
}