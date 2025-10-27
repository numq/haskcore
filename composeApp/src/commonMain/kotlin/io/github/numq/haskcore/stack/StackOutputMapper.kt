package io.github.numq.haskcore.stack

import io.github.numq.haskcore.output.OutputMessage

internal object StackOutputMapper {
    fun transform(output: StackOutput) = with(output) {
        val text = when (this) {
            is StackOutput.Progress -> "📝 $message"

            is StackOutput.Warning -> "⚠️ $message"

            is StackOutput.Error -> "❌ $message"

            is StackOutput.BuildModule -> "🔨 [$module] $message"

            is StackOutput.TestResult -> {
                val status = if (passed) "✅ PASS" else "❌ FAIL"

                "$status $module"
            }

            is StackOutput.RunOutput -> "🚀 $text"

            is StackOutput.Completion.Success -> {
                val time = duration.inWholeSeconds

                "✅ Completed successfully (${time}s)"
            }

            is StackOutput.Completion.Failure -> {
                val time = duration.inWholeSeconds

                "💥 Failed with exit code $exitCode (${time}s): $error"
            }
        }

        OutputMessage(text = text)
    }
}