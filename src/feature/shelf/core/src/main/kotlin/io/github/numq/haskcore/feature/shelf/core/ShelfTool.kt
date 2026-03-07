package io.github.numq.haskcore.feature.shelf.core

sealed interface ShelfTool {
    data object Explorer : ShelfTool

    data object Log : ShelfTool
}