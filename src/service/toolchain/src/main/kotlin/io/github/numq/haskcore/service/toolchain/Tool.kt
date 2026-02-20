package io.github.numq.haskcore.service.toolchain

sealed interface Tool {
    val binaryName: String

    val path: String

    val version: String

    data class Ghc(override val path: String, override val version: String) : Tool {
        override val binaryName = "ghc"
    }

    data class Cabal(override val path: String, override val version: String) : Tool {
        override val binaryName = "cabal"
    }

    data class Stack(override val path: String, override val version: String) : Tool {
        override val binaryName = "stack"
    }

    data class Hls(override val path: String, override val version: String) : Tool {
        override val binaryName = "haskell-language-server-wrapper"
    }
}