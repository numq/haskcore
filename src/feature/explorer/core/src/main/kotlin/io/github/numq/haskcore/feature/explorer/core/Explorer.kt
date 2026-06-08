package io.github.numq.haskcore.feature.explorer.core

data class Explorer(val expandedPaths: List<String> = emptyList(), val position: ExplorerPosition = ExplorerPosition())