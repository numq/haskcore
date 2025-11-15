package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.exception.BuildSystemException
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BuildCommand {
    companion object {
        private fun parseStackCommand(path: String, arguments: List<String>) = when (arguments.first().lowercase()) {
            "build" -> Stack.Build(path = path)

            "test" -> Stack.Test(path = path)

            "run" -> Stack.Run(path = path)

            "clean" -> Stack.Clean(path = path)

            else -> Stack.Custom(path = path, arguments = arguments)
        }

        private fun parseCabalCommand(path: String, arguments: List<String>) = when (arguments.first().lowercase()) {
            "build" -> Cabal.Build(path = path)

            "test" -> Cabal.Test(path = path)

            "run" -> Cabal.Run(path = path)

            "clean" -> Cabal.Clean(path = path)

            else -> Cabal.Custom(path = path, arguments = arguments)
        }

        private fun parseGhcCommand(path: String, arguments: List<String>) = when (arguments.first().lowercase()) {
            "compile" -> Ghc.Compile(path = path)

            "run" -> Ghc.Run(path = path)

            else -> Ghc.Custom(path = path, arguments = arguments)
        }

        private fun parseRunHaskellCommand(path: String, arguments: List<String>) =
            RunHaskell(path = path, arguments = arguments)

        fun parse(path: String, command: String) = runCatching {
            val parts = command.trim().split("\\s+".toRegex())

            if (parts.isEmpty()) {
                throw BuildSystemException("Empty command")
            }

            val arguments = parts.drop(1)

            if (arguments.isEmpty()) {
                throw BuildSystemException("Command requires arguments")
            }

            when (val command = parts.first().lowercase()) {
                "stack" -> parseStackCommand(path = path, arguments = arguments)

                "cabal" -> parseCabalCommand(path = path, arguments = arguments)

                "ghc" -> parseGhcCommand(path = path, arguments = arguments)

                "runhaskell" -> parseRunHaskellCommand(path = path, arguments = arguments)

                else -> Custom(path = path, command = listOf(command), arguments = arguments)
            }
        }
    }

    val path: String

    val command: List<String>

    val arguments: List<String>

    @Serializable
    sealed interface Stack : BuildCommand {
        override val command get() = listOf("stack")

        @Serializable
        data class Build(override val path: String) : Stack {
            override val arguments = listOf("build")
        }

        @Serializable
        data class Test(override val path: String) : Stack {
            override val arguments = listOf("test")
        }

        @Serializable
        data class Run(override val path: String) : Stack {
            override val arguments = listOf("run")
        }

        @Serializable
        data class Clean(override val path: String) : Stack {
            override val arguments = listOf("clean")
        }

        @Serializable
        data class Custom(override val path: String, override val arguments: List<String>) : Stack
    }

    @Serializable
    sealed interface Cabal : BuildCommand {
        override val command get() = listOf("stack", "exec", "cabal")

        @Serializable
        data class Build(override val path: String) : Cabal {
            override val arguments = listOf("build")
        }

        @Serializable
        data class Test(override val path: String) : Cabal {
            override val arguments = listOf("test")
        }

        @Serializable
        data class Run(override val path: String) : Cabal {
            override val arguments = listOf("run")
        }

        @Serializable
        data class Clean(override val path: String) : Cabal {
            override val arguments = listOf("clean")
        }

        @Serializable
        data class Custom(override val path: String, override val arguments: List<String>) : Cabal
    }

    @Serializable
    sealed interface Ghc : BuildCommand {
        override val command get() = listOf("stack", "exec", "ghc")

        @Serializable
        data class Compile(override val path: String) : Ghc {
            override val arguments = listOf("compile")
        }

        @Serializable
        data class Run(override val path: String) : Ghc {
            override val arguments = listOf("run")
        }

        @Serializable
        data class Custom(override val path: String, override val arguments: List<String>) : Ghc
    }

    @Serializable
    data class RunHaskell(override val path: String, override val arguments: List<String>) : BuildCommand {
        override val command get() = listOf("stack", "exec", "runhaskell")
    }

    @Serializable
    data class Custom(
        override val path: String, override val command: List<String>, override val arguments: List<String>
    ) : BuildCommand
}