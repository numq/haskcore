package io.github.numq.haskcore.feature.execution.core

sealed interface ExecutionArtifact {
    val target: ExecutionTarget

    sealed interface File : ExecutionArtifact {
        data class Haskell(override val target: ExecutionTarget) : File

        data class LiterateScript(override val target: ExecutionTarget) : File
    }

    data class Cabal(override val target: ExecutionTarget) : ExecutionArtifact

    data class Stack(override val target: ExecutionTarget) : ExecutionArtifact
}