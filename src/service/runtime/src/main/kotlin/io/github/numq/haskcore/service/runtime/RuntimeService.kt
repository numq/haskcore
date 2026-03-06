package io.github.numq.haskcore.service.runtime

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface RuntimeService : AutoCloseable {
    val events: SharedFlow<RuntimeEvent>

    suspend fun execute(request: RuntimeRequest): Either<Throwable, Flow<RuntimeEvent>>

    suspend fun start(request: RuntimeRequest): Either<Throwable, Unit>

    suspend fun stop(id: String): Either<Throwable, Unit>
}