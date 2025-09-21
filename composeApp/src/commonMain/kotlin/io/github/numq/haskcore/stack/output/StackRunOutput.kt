package io.github.numq.haskcore.stack.output

import kotlin.time.Duration

sealed interface StackRunOutput {
    data class Output(val text: String) : StackRunOutput

    data class Completion(val exitCode: Int, val duration: Duration) : StackRunOutput
}