package io.github.numq.haskcore.feature.navigation.core

internal fun WorkspaceData.toInitialWorkspace() = InitialWorkspace(
    x = x, y = y, width = width, height = height, isFullscreen = isFullscreen, ratio = ratio
)