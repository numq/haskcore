package io.github.numq.haskcore.toolchain

import io.github.numq.haskcore.buildtool.BuildTool

internal sealed interface Toolchain {
    data class Uninitialized(val ghcPath: String? = null, val cabalPath: String? = null, val stackPath: String? = null) : Toolchain

    data class Initializing(val ghcPath: String? = null, val cabalPath: String? = null, val stackPath: String? = null) : Toolchain

    data class Initialized(
        val cabal: BuildTool.Cabal?,
        val ghc: BuildTool.Ghc?,
        val stack: BuildTool.Stack?
    ) : Toolchain
}