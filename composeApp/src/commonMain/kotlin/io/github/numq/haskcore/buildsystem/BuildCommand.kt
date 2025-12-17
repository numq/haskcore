package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.buildsystem.stack.StackTemplate

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

    sealed interface Stack : BuildCommand {
        override val command get() = listOf("stack")

        data class New(override val path: String, val bare: Boolean = false, val template: StackTemplate? = null) :
            Stack {
            override val arguments = buildList {
                add("new")

                if (bare) add("--bare")

                if (template != null) add(template.title)
            }
        }

        data class Build(override val path: String) : Stack {
            override val arguments = listOf("build")
        }

        data class Test(override val path: String) : Stack {
            override val arguments = listOf("test")
        }

        data class Run(override val path: String) : Stack {
            override val arguments = listOf("run")
        }

        data class Clean(override val path: String) : Stack {
            override val arguments = listOf("clean")
        }

        data class Custom(override val path: String, override val arguments: List<String>) : Stack
    }

    sealed interface Cabal : BuildCommand {
        override val command get() = listOf("stack", "exec", "cabal")

        data class Init(override val path: String) : Cabal {
            override val arguments = listOf("init")
        }

        data class Build(override val path: String) : Cabal {
            override val arguments = listOf("build")
        }

        data class Test(override val path: String) : Cabal {
            override val arguments = listOf("test")
        }

        data class Run(override val path: String) : Cabal {
            override val arguments = listOf("run")
        }

        data class Clean(override val path: String) : Cabal {
            override val arguments = listOf("clean")
        }

        data class Custom(override val path: String, override val arguments: List<String>) : Cabal
    }

    sealed interface Ghc : BuildCommand {
        override val command get() = listOf("stack", "exec", "ghc")

        data class Compile(override val path: String) : Ghc {
            override val arguments = listOf("compile")
        }

        data class Run(override val path: String) : Ghc {
            override val arguments = listOf("run")
        }

        data class Custom(override val path: String, override val arguments: List<String>) : Ghc
    }

    data class RunHaskell(override val path: String, override val arguments: List<String>) : BuildCommand {
        override val command get() = listOf("stack", "exec", "runhaskell")
    }

    data class Custom(
        override val path: String, override val command: List<String>, override val arguments: List<String>
    ) : BuildCommand
}