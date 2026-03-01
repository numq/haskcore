package io.github.numq.haskcore.service.logger

import arrow.core.Either
import io.github.numq.haskcore.core.log.Log
import kotlinx.coroutines.flow.StateFlow

interface LoggerService : AutoCloseable {
    val logs: StateFlow<List<Log>>

    suspend fun submit(log: Log): Either<Throwable, Unit>

    suspend fun export(path: String): Either<Throwable, Unit>

    suspend fun clear(): Either<Throwable, Unit>
}