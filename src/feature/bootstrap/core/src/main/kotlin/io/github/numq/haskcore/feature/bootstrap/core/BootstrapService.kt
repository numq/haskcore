package io.github.numq.haskcore.feature.bootstrap.core

import arrow.core.Either

interface BootstrapService {
    suspend fun initialize(block: suspend () -> Bootstrap): Either<Throwable, Bootstrap>
}