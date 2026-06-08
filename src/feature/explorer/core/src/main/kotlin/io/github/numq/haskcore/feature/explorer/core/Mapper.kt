package io.github.numq.haskcore.feature.explorer.core

internal fun ExplorerPositionData.toExplorerPosition() = ExplorerPosition(index = index, offset = offset)

internal fun ExplorerData.toExplorer() = Explorer(
    expandedPaths = expandedPaths, position = position.toExplorerPosition()
)