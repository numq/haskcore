package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp
import kotlin.time.Duration

internal sealed interface BuildOutput {
    val message: String

    val timestamp: Timestamp

    data class ProgressOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    data class BuildModuleOutput(
        val module: String, override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    data class TestResultOutput(
        val module: String,
        val passed: Boolean,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    data class WarningOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    data class ErrorOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    data class RunOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput

    sealed interface CompletionOutput : BuildOutput {
        val exitCode: Int

        val duration: Duration

        data class Success(
            override val exitCode: Int,
            override val duration: Duration,
            override val timestamp: Timestamp = Timestamp.now()
        ) : CompletionOutput {
            override val message: String = "Build completed successfully in ${duration.inWholeSeconds}s"
        }

        data class Failure(
            override val exitCode: Int,
            override val duration: Duration,
            val error: String,
            override val timestamp: Timestamp = Timestamp.now()
        ) : CompletionOutput {
            override val message: String = "Build failed: $error (exit code: $exitCode)"
        }
    }
}