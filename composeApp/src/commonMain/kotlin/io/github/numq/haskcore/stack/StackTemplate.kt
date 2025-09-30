package io.github.numq.haskcore.stack

internal sealed interface StackTemplate {
    companion object {
        val values = setOf(Simple, Library, Executable, TestSuite, Full)
    }

    val name: String

    data object Simple : StackTemplate {
        override val name = "simple"
    }

    data object Library : StackTemplate {
        override val name = "library"
    }

    data object Executable : StackTemplate {
        override val name = "executable"
    }

    data object TestSuite : StackTemplate {
        override val name = "test-suite"
    }

    data object Full : StackTemplate {
        override val name = "full"
    }
}