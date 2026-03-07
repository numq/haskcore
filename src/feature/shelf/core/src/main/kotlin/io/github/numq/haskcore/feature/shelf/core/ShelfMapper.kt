package io.github.numq.haskcore.feature.shelf.core

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