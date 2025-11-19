package io.github.numq.haskcore.explorer

internal sealed interface Explorer {
    data object Loading : Explorer

    data class Loaded(val rootPath: String, val nodes: List<ExplorerNode> = emptyList()) : Explorer
}