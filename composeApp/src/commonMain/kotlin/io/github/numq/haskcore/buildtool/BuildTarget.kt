package io.github.numq.haskcore.buildtool

sealed interface BuildTarget {
    data object Benchmark : BuildTarget

    data object Executable : BuildTarget

    data object Library : BuildTarget

    data object Test : BuildTarget
}