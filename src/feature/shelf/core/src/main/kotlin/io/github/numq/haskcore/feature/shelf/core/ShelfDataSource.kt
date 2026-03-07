package io.github.numq.haskcore.feature.shelf.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ShelfDataSource : AutoCloseable {
    val shelfData: Flow<ShelfData>

    suspend fun get(): Either<Throwable, ShelfData>

    suspend fun update(transform: (ShelfData) -> ShelfData): Either<Throwable, ShelfData>
}