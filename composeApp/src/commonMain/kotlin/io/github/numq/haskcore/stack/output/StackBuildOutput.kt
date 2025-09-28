package io.github.numq.haskcore.stack.output

import kotlin.time.Duration

internal sealed interface StackBuildOutput {
    data class Progress(val module: String, val message: String) : StackBuildOutput

    data class Warning(val message: String) : StackBuildOutput

    data class Error(val message: String) : StackBuildOutput

    data class Completion(val exitCode: Int, val duration: Duration) : StackBuildOutput
}