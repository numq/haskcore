package io.github.numq.haskcore.service.toolchain

import arrow.core.Either

sealed interface Toolchain {
    data object Scanning : Toolchain

    data object NotFound : Toolchain

    data class Detected(
        val ghc: Either<Throwable, Tool.Ghc>,
        val cabal: Either<Throwable, Tool.Cabal>,
        val stack: Either<Throwable, Tool.Stack>,
        val hls: Either<Throwable, Tool.Hls>
    ) : Toolchain

    data class Error(val throwable: Throwable) : Toolchain
}