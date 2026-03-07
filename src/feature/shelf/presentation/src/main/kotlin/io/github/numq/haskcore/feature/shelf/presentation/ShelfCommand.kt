package io.github.numq.haskcore.feature.shelf.presentation

import io.github.numq.haskcore.feature.shelf.core.Shelf
import io.github.numq.haskcore.feature.shelf.core.ShelfTool
import kotlinx.coroutines.flow.Flow

internal sealed interface ShelfCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_SUCCESS, SELECT_SHELF_TOOL, SAVE_LEFT_RATIO, SAVE_RIGHT_RATIO
    }

    data class HandleFailure(val throwable: Throwable) : ShelfCommand

    data object Initialize : ShelfCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeSuccess(val flow: Flow<Shelf>) : ShelfCommand {
        val key = Key.INITIALIZE_SUCCESS
    }

    data class UpdateShelf(val shelf: Shelf) : ShelfCommand

    data class SelectShelfTool(val tool: ShelfTool) : ShelfCommand {
        val key = Key.SELECT_SHELF_TOOL
    }

    data object SelectShelfToolSuccess : ShelfCommand

    data class SaveLeftRatio(val ratio: Float) : ShelfCommand {
        val key = Key.SAVE_LEFT_RATIO
    }

    data object SaveLeftRatioSuccess : ShelfCommand

    data class SaveRightRatio(val ratio: Float) : ShelfCommand {
        val key = Key.SAVE_RIGHT_RATIO
    }

    data object SaveRightRatioSuccess : ShelfCommand
}