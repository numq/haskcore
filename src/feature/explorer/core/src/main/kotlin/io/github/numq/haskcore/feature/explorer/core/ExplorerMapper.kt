package io.github.numq.haskcore.feature.explorer.core

internal fun ExplorerData.toExplorer() = Explorer(expandedPaths = expandedPaths, index = index, offset = offset)