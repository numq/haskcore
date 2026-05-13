package io.github.numq.haskcore.feature.workspace.core

sealed interface ShelfTool {
    data object Explorer : ShelfTool

    data object Log : ShelfTool
}