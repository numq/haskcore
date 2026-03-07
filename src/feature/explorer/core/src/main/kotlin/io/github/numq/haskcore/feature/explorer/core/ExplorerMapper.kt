package io.github.numq.haskcore.feature.explorer.core

internal fun ExplorerData.toExplorer() = Explorer(
    expandedPaths = expandedPaths, selectedPath = selectedPath, index = index, offset = offset
)