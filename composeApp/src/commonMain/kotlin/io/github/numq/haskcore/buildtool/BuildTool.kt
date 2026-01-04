package io.github.numq.haskcore.buildtool

internal sealed interface BuildTool {
    val path: String

    val version: String

    data class Cabal(override val path: String, override val version: String) : BuildTool

    data class Ghc(override val path: String, override val version: String) : BuildTool

    data class Stack(override val path: String, override val version: String) : BuildTool
}