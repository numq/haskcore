package io.github.numq.haskcore.explorer

sealed interface ExplorerNode {
    val name: String

    val path: String

    val parentPath: String

    val depth: Int

    val lastModified: Long

    val cut: Boolean

    data class File(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val depth: Int,
        override val lastModified: Long,
        override val cut: Boolean,
        val extension: String
    ) : ExplorerNode

    data class Directory(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val depth: Int,
        override val lastModified: Long,
        override val cut: Boolean,
        val expanded: Boolean
    ) : ExplorerNode
}