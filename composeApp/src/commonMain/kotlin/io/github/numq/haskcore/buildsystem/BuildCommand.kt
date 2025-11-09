package io.github.numq.haskcore.buildsystem

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BuildCommand {
    val path: String

    val fullCommand: List<String>

    @Serializable
    sealed interface Stack : BuildCommand {
        @Serializable
        data class Build(override val path: String) : Stack {
            override val fullCommand = listOf("build")
        }

        @Serializable
        data class Test(override val path: String) : Stack {
            override val fullCommand = listOf("test")
        }

        @Serializable
        data class Run(override val path: String) : Stack {
            override val fullCommand = listOf("run")
        }

        @Serializable
        data class Clean(override val path: String) : Stack {
            override val fullCommand = listOf("clean")
        }

        @Serializable
        data class Custom(override val path: String, override val fullCommand: List<String>) : Stack
    }

    @Serializable
    sealed interface Cabal : BuildCommand {
        @Serializable
        data class Build(override val path: String) : Cabal {
            override val fullCommand = listOf("build")
        }

        @Serializable
        data class Test(override val path: String) : Cabal {
            override val fullCommand = listOf("test")
        }

        @Serializable
        data class Run(override val path: String) : Cabal {
            override val fullCommand = listOf("run")
        }

        @Serializable
        data class Clean(override val path: String) : Cabal {
            override val fullCommand = listOf("clean")
        }

        @Serializable
        data class Custom(override val path: String, override val fullCommand: List<String>) : Cabal
    }

    @Serializable
    sealed interface Ghc : BuildCommand {
        @Serializable
        data class Compile(override val path: String) : Ghc {
            override val fullCommand = listOf("compile")
        }

        @Serializable
        data class Run(override val path: String) : Ghc {
            override val fullCommand = listOf("run")
        }

        @Serializable
        data class Custom(override val path: String, override val fullCommand: List<String>) : Ghc
    }

    @Serializable
    sealed interface RunHaskell : BuildCommand {
        @Serializable
        data class Run(override val path: String) : RunHaskell {
            override val fullCommand = listOf("exec", "runhaskell")
        }

        @Serializable
        data class Custom(override val path: String, override val fullCommand: List<String>) : RunHaskell
    }

    @Serializable
    data class Custom(override val path: String, override val fullCommand: List<String>) : BuildCommand
}