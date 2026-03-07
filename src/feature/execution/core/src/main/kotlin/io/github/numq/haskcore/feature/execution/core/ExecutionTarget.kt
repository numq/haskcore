package io.github.numq.haskcore.feature.execution.core

sealed interface ExecutionTarget {
    val path: String

    val name: String

    val extension: String

    data class Benchmark(
        override val path: String, override val name: String, override val extension: String
    ) : ExecutionTarget

    data class Executable(
        override val path: String, override val name: String, override val extension: String
    ) : ExecutionTarget

    data class Test(
        override val path: String, override val name: String, override val extension: String
    ) : ExecutionTarget
}