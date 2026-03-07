package io.github.numq.haskcore.feature.bootstrap.core

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

internal class DefaultBootstrapService : BootstrapService {
    private companion object {
        const val REQUIRED_DELAY_MILLIS = 1_000L
    }

    override suspend fun initialize(block: suspend () -> Bootstrap): Either<Throwable, Bootstrap> = either {
        coroutineScope {
            val timer = async {
                delay(REQUIRED_DELAY_MILLIS)
            }

            val bootstrap = async { block() }

            timer.await()

            bootstrap.await()
        }
    }
}