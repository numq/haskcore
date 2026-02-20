package io.github.numq.haskcore.service.toolchain

import arrow.core.Either

sealed interface Toolchain {
    data object Scanning : Toolchain

    data object NotFound : Toolchain

    data class Detected(
        val ghc: Either<Throwable, Tool>,
        val cabal: Either<Throwable, Tool>,
        val stack: Either<Throwable, Tool>,
        val hls: Either<Throwable, Tool>
    ) : Toolchain

    data class Error(val throwable: Throwable) : Toolchain
}