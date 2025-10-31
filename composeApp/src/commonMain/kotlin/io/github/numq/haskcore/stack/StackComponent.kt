package io.github.numq.haskcore.stack

internal sealed interface StackComponent {
    val path: String

    val name: String

    val type: StackComponentType

    data class Library(
        override val path: String,
        override val name: String,
        val exposedModules: List<String>
    ) : StackComponent {
        override val type = StackComponentType.LIBRARY
    }

    data class Executable(
        override val path: String,
        override val name: String,
        val mainFile: String?
    ) : StackComponent {
        override val type = StackComponentType.EXECUTABLE
    }

    data class Test(
        override val path: String,
        override val name: String,
        val mainFile: String?
    ) : StackComponent {
        override val type = StackComponentType.TEST
    }

    data class Benchmark(
        override val path: String,
        override val name: String,
        val mainFile: String?
    ) : StackComponent {
        override val type = StackComponentType.BENCHMARK
    }
}