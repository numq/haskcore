package io.github.numq.haskcore.feature.explorer.core

data class ExplorerTree(
    val root: ExplorerRoot,
    val nodes: List<ExplorerNode> = emptyList(),
    val position: ExplorerPosition = ExplorerPosition(),
)