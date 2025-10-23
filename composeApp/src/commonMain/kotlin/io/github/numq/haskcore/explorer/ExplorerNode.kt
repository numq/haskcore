package io.github.numq.haskcore.explorer

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ExplorerNode {
    val name: String

    val path: String

    val parentPath: String

    val depth: Int

    val lastModified: Long

    @Serializable
    data class File(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val depth: Int,
        override val lastModified: Long,
        val extension: String
    ) : ExplorerNode

    @Serializable
    data class Directory(
        override val name: String,
        override val path: String,
        override val parentPath: String,
        override val depth: Int,
        override val lastModified: Long,
        val expanded: Boolean
    ) : ExplorerNode
}