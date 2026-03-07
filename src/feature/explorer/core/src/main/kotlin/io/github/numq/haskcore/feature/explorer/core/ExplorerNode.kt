package io.github.numq.haskcore.feature.explorer.core

sealed interface ExplorerNode {
    val name: String
    val path: String
    val level: Int
    val isSelected: Boolean

    data class File(
        override val name: String,
        override val path: String,
        override val level: Int,
        override val isSelected: Boolean = false,
        val extension: String? = null
    ) : ExplorerNode

    data class Directory(
        override val name: String,
        override val path: String,
        override val level: Int,
        override val isSelected: Boolean = false,
        val isExpanded: Boolean = false
    ) : ExplorerNode
}