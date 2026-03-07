package io.github.numq.haskcore.feature.explorer.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ExplorerDataSource : AutoCloseable {
    val explorerData: Flow<ExplorerData>

    suspend fun get(): Either<Throwable, ExplorerData>

    suspend fun update(transform: (ExplorerData) -> ExplorerData): Either<Throwable, ExplorerData>
}