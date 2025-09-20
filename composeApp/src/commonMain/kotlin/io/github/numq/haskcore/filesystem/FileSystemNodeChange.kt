package io.github.numq.haskcore.filesystem

sealed interface FileSystemNodeChange {
    data class Added(val node: FileSystemNode) : FileSystemNodeChange

    data class Updated(val node: FileSystemNode) : FileSystemNodeChange

    data class Removed(val path: String) : FileSystemNodeChange
}