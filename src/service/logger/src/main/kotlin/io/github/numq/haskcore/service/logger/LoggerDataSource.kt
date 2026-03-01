package io.github.numq.haskcore.service.logger

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface LoggerDataSource : AutoCloseable {
    val loggerData: Flow<List<LoggerData>>

    suspend fun get(): Either<Throwable, List<LoggerData>>

    suspend fun update(transform: (List<LoggerData>) -> List<LoggerData>): Either<Throwable, List<LoggerData>?>
}