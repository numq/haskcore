package io.github.numq.haskcore.feature.explorer.presentation.dialog

import io.github.numq.haskcore.feature.explorer.core.ExplorerNode

internal sealed interface ExplorerDialog {
    data object None : ExplorerDialog

    data class CreateFile(
        val node: ExplorerNode, val name: String = "", val error: String? = null,
    ) : ExplorerDialog

    data class CreateDirectory(
        val node: ExplorerNode, val name: String = "", val error: String? = null,
    ) : ExplorerDialog
}