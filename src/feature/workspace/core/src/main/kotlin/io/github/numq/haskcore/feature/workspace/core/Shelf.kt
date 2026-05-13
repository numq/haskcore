package io.github.numq.haskcore.feature.workspace.core

import arrow.core.nonEmptyListOf

data class Shelf(
    val leftPanel: ShelfPanel = ShelfPanel(tools = nonEmptyListOf(ShelfTool.Explorer)),
    val rightPanel: ShelfPanel = ShelfPanel(tools = nonEmptyListOf(ShelfTool.Log)),
)