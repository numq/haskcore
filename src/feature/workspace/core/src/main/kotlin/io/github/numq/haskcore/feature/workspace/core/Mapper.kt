package io.github.numq.haskcore.feature.workspace.core

import arrow.core.toNonEmptyListOrThrow

internal fun ShelfToolData.toShelfTool() = when (this) {
    is ShelfToolData.Explorer -> ShelfTool.Explorer

    is ShelfToolData.Log -> ShelfTool.Log
}

internal fun ShelfTool.toShelfToolData() = when (this) {
    is ShelfTool.Explorer -> ShelfToolData.Explorer

    is ShelfTool.Log -> ShelfToolData.Log
}

internal fun ShelfPanelData.toShelfPanel() = ShelfPanel(
    tools = tools.map(ShelfToolData::toShelfTool).toNonEmptyListOrThrow(), // todo
    activeTool = activeTool?.toShelfTool()
)

internal fun ShelfData.toShelf() = Shelf(leftPanel = leftPanel.toShelfPanel(), rightPanel = rightPanel.toShelfPanel())

internal fun WorkspaceData.toWorkspace() = Workspace(
    x = x ?: Workspace.DEFAULT_X,
    y = y ?: Workspace.DEFAULT_Y,
    width = width ?: Workspace.DEFAULT_WIDTH,
    height = height ?: Workspace.DEFAULT_HEIGHT,
    isFullscreen = isFullscreen,
    verticalRatio = verticalRatio ?: Workspace.DEFAULT_VERTICAL_RATIO,
    shelf = shelfData?.toShelf() ?: Shelf(),
)