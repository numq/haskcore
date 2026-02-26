package io.github.numq.haskcore.service.runtime

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface RuntimeService {
    suspend fun execute(request: RuntimeRequest): Either<Throwable, Flow<RuntimeEvent>>
}