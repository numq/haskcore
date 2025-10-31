package io.github.numq.haskcore.stack

internal data class StackProject(
    val path: String,
    val name: String,
    val packages: List<StackPackage>,
    val resolver: String,
    val ghcVersion: String?
)