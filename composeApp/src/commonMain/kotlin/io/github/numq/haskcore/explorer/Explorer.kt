package io.github.numq.haskcore.explorer

import kotlinx.serialization.Serializable

@Serializable
internal data class Explorer(
    val path: String,
    val nodes: List<ExplorerNode> = emptyList(),
    val expandedDirectories: Set<String> = emptySet(),
    val selectedNodes: Set<String> = emptySet()
)