package io.github.numq.haskcore.service.syntax.initializer

import arrow.core.Either

interface SyntaxServiceInitializer {
    suspend fun initialize(): Either<Throwable, Unit>
}