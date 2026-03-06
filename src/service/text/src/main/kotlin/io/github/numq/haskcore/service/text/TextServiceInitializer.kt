package io.github.numq.haskcore.service.text

import arrow.core.Either

interface TextServiceInitializer {
    suspend fun initialize(): Either<Throwable, Unit>
}