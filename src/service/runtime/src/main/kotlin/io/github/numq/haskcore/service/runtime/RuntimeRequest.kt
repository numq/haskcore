package io.github.numq.haskcore.service.runtime

sealed class RuntimeRequest(
    val id: String, val name: String, val command: String, val arguments: List<String>, val workingDir: String? = null
) {
    class RunGHC(
        id: String, name: String, path: String, arguments: List<String>
    ) : RuntimeRequest(id = id, name = name, command = "runghc", arguments = listOf(path) + arguments)

    class Cabal(
        id: String, name: String, arguments: List<String>
    ) : RuntimeRequest(id = id, name = name, command = "cabal", arguments = arguments)

    class Stack(
        id: String, name: String, arguments: List<String>
    ) : RuntimeRequest(id = id, name = name, command = "stack", arguments = arguments)
}