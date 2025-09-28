package io.github.numq.haskcore.stack.output

import kotlin.time.Duration

internal sealed interface StackDependencyOutput {
    data class Info(val name: String, val version: String) : StackDependencyOutput

    data class Completion(val exitCode: Int, val duration: Duration) : StackDependencyOutput
}