package io.github.numq.haskcore.feature.explorer.core

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ExplorerService {
    val explorer: StateFlow<Explorer>

    suspend fun toggleNode(path: String): Either<Throwable, Unit>

    suspend fun selectNode(path: String): Either<Throwable, Unit>

    suspend fun saveExplorerPosition(position: ExplorerPosition): Either<Throwable, Unit>
}