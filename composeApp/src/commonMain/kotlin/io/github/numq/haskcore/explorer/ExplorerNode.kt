package io.github.numq.haskcore.explorer

internal sealed interface ExplorerNode {
    val path: String

    val name: String

    val parentPath: String

    val parentName: String

    val depth: Int

    val lastModified: Long

    data class File(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val parentName: String,
        override val depth: Int,
        override val lastModified: Long,
        val extension: String,
        val nameWithoutExtension: String
    ) : ExplorerNode

    data class Directory(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val parentName: String,
        override val depth: Int,
        override val lastModified: Long,
        val expanded: Boolean
    ) : ExplorerNode
}