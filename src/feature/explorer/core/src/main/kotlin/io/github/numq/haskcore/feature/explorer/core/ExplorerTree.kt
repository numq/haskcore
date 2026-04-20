package io.github.numq.haskcore.feature.explorer.core

sealed interface ExplorerTree {
    val root: ExplorerRoot

    data class Loading(override val root: ExplorerRoot) : ExplorerTree

    data class Loaded(
        override val root: ExplorerRoot, val nodes: List<ExplorerNode>, val position: ExplorerPosition,
    ) : ExplorerTree
}