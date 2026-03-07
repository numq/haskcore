package io.github.numq.haskcore.feature.workspace.core

internal fun WorkspaceData.toWorkspace() = Workspace(
    x = x, y = y, width = width, height = height, isFullscreen = isFullscreen, ratio = ratio ?: Workspace.DEFAULT_RATIO
)