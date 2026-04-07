package io.github.numq.haskcore.service.runtime

sealed interface RuntimeRequest {
    private companion object {
        const val STACK_COMMAND = "stack"

        const val CABAL_COMMAND = "cabal"

        const val GHC_COMMAND = "runghc"
    }

    val id: String

    val name: String

    val command: String

    val arguments: List<String>

    val workingDir: String?

    val env: Map<String, String>

    data class Stack(
        override val id: String,
        override val name: String,
        override val arguments: List<String>,
        override val workingDir: String?,
        override val env: Map<String, String> = emptyMap()
    ) : RuntimeRequest {
        override val command = STACK_COMMAND
    }

    data class Cabal(
        override val id: String,
        override val name: String,
        override val arguments: List<String>,
        override val workingDir: String?,
        override val env: Map<String, String> = emptyMap()
    ) : RuntimeRequest {
        override val command = CABAL_COMMAND
    }

    data class Ghc(
        override val id: String,
        override val name: String,
        override val arguments: List<String>,
        override val workingDir: String?,
        override val env: Map<String, String> = emptyMap()
    ) : RuntimeRequest {
        override val command = GHC_COMMAND
    }

    data class Custom(
        override val id: String,
        override val name: String,
        override val command: String,
        override val arguments: List<String>,
        override val workingDir: String?,
        override val env: Map<String, String> = emptyMap()
    ) : RuntimeRequest
}