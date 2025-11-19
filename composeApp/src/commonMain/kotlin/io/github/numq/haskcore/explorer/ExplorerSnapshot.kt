package io.github.numq.haskcore.explorer

import kotlinx.serialization.Serializable

@Serializable
internal data class ExplorerSnapshot(val expandedDirectoryPaths: List<String> = emptyList())