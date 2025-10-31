package io.github.numq.haskcore.stack

internal data class StackPackage(
    val path: String, val name: String, val components: List<StackComponent>, val configFile: String
)