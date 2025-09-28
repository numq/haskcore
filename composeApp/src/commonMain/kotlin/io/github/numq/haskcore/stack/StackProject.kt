package io.github.numq.haskcore.stack

internal data class StackProject(
    val path: String,
    val name: String,
    val resolver: String,
    val ghcVersion: String,
    val dependencies: List<String> = emptyList(),
    val targets: List<String> = emptyList(),
    val flags: Map<String, Boolean> = emptyMap()
)