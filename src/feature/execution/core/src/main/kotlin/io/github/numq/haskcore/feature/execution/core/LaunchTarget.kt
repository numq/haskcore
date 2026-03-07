package io.github.numq.haskcore.feature.execution.core

sealed interface LaunchTarget {
    val name: String

    val workingDir: String

    data class SingleFile(
        override val name: String, override val workingDir: String, val filePath: String
    ) : LaunchTarget

    data class Cabal(
        override val name: String, override val workingDir: String, val componentName: String
    ) : LaunchTarget

    data class Stack(
        override val name: String, override val workingDir: String, val componentName: String
    ) : LaunchTarget
}