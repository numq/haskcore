package io.github.numq.haskcore.buildsystem

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BuildTarget {
    val path: String

    val name: String

    @Serializable
    sealed interface BuildProject : BuildTarget {
        @Serializable
        data class Stack(override val path: String, override val name: String) : BuildProject

        @Serializable
        data class Cabal(override val path: String, override val name: String) : BuildProject
    }

    @Serializable
    data class HaskellFile(override val path: String, override val name: String) : BuildTarget

    @Serializable
    data class LiterateScript(override val path: String, override val name: String) : BuildTarget
}