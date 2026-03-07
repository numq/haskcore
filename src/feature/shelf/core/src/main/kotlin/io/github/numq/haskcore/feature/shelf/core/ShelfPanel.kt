package io.github.numq.haskcore.feature.shelf.core

import arrow.core.NonEmptyList

data class ShelfPanel(
    val tools: NonEmptyList<ShelfTool>, val activeTool: ShelfTool? = null, val ratio: Float = DEFAULT_RATIO
) {
    companion object {
        const val DEFAULT_RATIO = .25f
    }
}