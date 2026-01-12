package io.github.numq.haskcore.output

import io.github.numq.haskcore.buildsystem.BuildOutput

internal fun BuildOutput.toOutputMessage() = when (this) {
    is BuildOutput.Line -> when {
        text.contains("error", ignoreCase = true) -> OutputMessage.Error(text = text)

        text.contains("warning", ignoreCase = true) -> OutputMessage.Warning(text = text)

        text.contains("success", ignoreCase = true) || text.contains(
            "done", ignoreCase = true
        ) -> OutputMessage.Success(text = text)

        else -> OutputMessage.Info(text = text)
    }

    is BuildOutput.Completion -> when (exitCode) {
        0 -> OutputMessage.Success("Completed successfully in $duration")

        else -> OutputMessage.Error("Failed with exit code $exitCode in $duration")
    }
}