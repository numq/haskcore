package io.github.numq.haskcore.feature.explorer.core

sealed interface ExplorerNode {
    val name: String

    val path: String

    val level: Int

    val segments: List<String>

    data class File(
        override val name: String,
        override val path: String,
        override val level: Int,
        override val segments: List<String>,
        val extension: String? = null
    ) : ExplorerNode

    data class Directory(
        override val name: String,
        override val path: String,
        override val level: Int,
        override val segments: List<String>,
        val isExpanded: Boolean = false
    ) : ExplorerNode
}