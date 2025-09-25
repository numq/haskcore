package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.filesystem.FileSystemChange

sealed interface ExplorerOperation {
    data class Change(val change: FileSystemChange) : ExplorerOperation

    data class Expand(val directory: ExplorerNode.Directory) : ExplorerOperation

    data class Collapse(val directory: ExplorerNode.Directory) : ExplorerOperation
}