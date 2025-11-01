package io.github.numq.haskcore.output

import io.github.numq.haskcore.buildsystem.BuildOutput
import io.github.numq.haskcore.stack.StackOutput

internal fun StackOutput.toBuildOutput() = when (this) {
    is StackOutput.Progress -> BuildOutput.ProgressOutput(message = message)

    is StackOutput.BuildModule -> BuildOutput.BuildModuleOutput(module = module, message = message)

    is StackOutput.Warning -> BuildOutput.WarningOutput(message = message)

    is StackOutput.Error -> BuildOutput.ErrorOutput(message = message)

    is StackOutput.RunOutput -> BuildOutput.RunOutput(message = message)

    is StackOutput.TestResult -> BuildOutput.TestResultOutput(module = module, passed = passed, message = message)

    is StackOutput.Completion.Success -> BuildOutput.CompletionOutput.Success(
        exitCode = exitCode, duration = duration
    )

    is StackOutput.Completion.Failure -> BuildOutput.CompletionOutput.Failure(
        exitCode = exitCode, duration = duration, error = error
    )
}