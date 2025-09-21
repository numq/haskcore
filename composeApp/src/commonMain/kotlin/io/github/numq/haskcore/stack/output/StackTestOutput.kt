package io.github.numq.haskcore.stack.output

import kotlin.time.Duration

sealed interface StackTestOutput {
    data class Result(val module: String, val passed: Boolean, val details: String) : StackTestOutput

    data class Warning(val message: String) : StackTestOutput

    data class Error(val message: String) : StackTestOutput

    data class Completion(val exitCode: Int, val duration: Duration) : StackTestOutput
}