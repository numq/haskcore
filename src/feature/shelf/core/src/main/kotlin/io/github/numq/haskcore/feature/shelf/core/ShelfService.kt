package io.github.numq.haskcore.feature.shelf.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ShelfService : AutoCloseable {
    val shelf: StateFlow<Shelf>

    suspend fun updateLeftRatio(ratio: Float): Either<Throwable, Unit>

    suspend fun updateRightRatio(ratio: Float): Either<Throwable, Unit>

    suspend fun selectShelfTool(tool: ShelfTool): Either<Throwable, Unit>
}