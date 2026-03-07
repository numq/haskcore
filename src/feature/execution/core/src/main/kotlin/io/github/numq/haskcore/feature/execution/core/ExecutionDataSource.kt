package io.github.numq.haskcore.feature.execution.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ExecutionDataSource : AutoCloseable {
    val executionData: Flow<ExecutionData>

    suspend fun get(): Either<Throwable, ExecutionData>

    suspend fun update(transform: (ExecutionData) -> ExecutionData): Either<Throwable, ExecutionData>
}