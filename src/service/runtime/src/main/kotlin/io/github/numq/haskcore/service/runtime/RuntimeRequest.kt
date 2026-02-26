package io.github.numq.haskcore.service.runtime

sealed class RuntimeRequest(val command: String, val arguments: List<String>, val workingDir: String? = null) {
    class RunGHC(
        path: String, arguments: List<String>
    ) : RuntimeRequest(command = "runghc", arguments = listOf(path) + arguments)

    class Cabal(arguments: List<String>) : RuntimeRequest(command = "cabal", arguments = arguments)

    class Stack(arguments: List<String>) : RuntimeRequest(command = "stack", arguments = arguments)
}