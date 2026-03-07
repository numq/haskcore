package io.github.numq.haskcore.feature.output.core

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface OutputDataSource : AutoCloseable {
    val outputData: Flow<OutputData>

    suspend fun get(): Either<Throwable, OutputData>

    suspend fun update(transform: (OutputData) -> OutputData): Either<Throwable, OutputData?>
}