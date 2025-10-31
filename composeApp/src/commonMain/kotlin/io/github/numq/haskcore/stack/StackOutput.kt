package io.github.numq.haskcore.stack

import kotlin.time.Duration

internal sealed interface StackOutput {
    data class Progress(val message: String) : StackOutput

    data class Warning(val message: String) : StackOutput

    data class Error(val message: String) : StackOutput

    data class BuildModule(val module: String, val message: String) : StackOutput

    data class RunOutput(val message: String) : StackOutput

    data class TestResult(val module: String, val passed: Boolean, val message: String) : StackOutput

    sealed interface Completion : StackOutput {
        val exitCode: Int

        val duration: Duration

        data class Success(override val exitCode: Int, override val duration: Duration) : Completion

        data class Failure(override val exitCode: Int, override val duration: Duration, val error: String) : Completion
    }
}