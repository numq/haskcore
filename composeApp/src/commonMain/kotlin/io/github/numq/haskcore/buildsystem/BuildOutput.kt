package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp
import java.util.*
import kotlin.time.Duration

internal sealed interface BuildOutput {
    val id: String

    val message: String

    val timestamp: Timestamp

    data class ProgressOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "progress-${UUID.randomUUID()}"
    }

    data class BuildModuleOutput(
        val module: String,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now(),
    ) : BuildOutput {
        override val id: String = "build-$module-${UUID.randomUUID()}"
    }

    data class TestResultOutput(
        val module: String,
        val passed: Boolean,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now(),
    ) : BuildOutput {
        override val id: String = "test-$module-${UUID.randomUUID()}"
    }

    data class WarningOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "warning-${UUID.randomUUID()}"
    }

    data class ErrorOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "error-${UUID.randomUUID()}"
    }

    data class RunOutput(
        override val message: String, override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "run-${UUID.randomUUID()}"
    }

    sealed interface CompletionOutput : BuildOutput {
        val exitCode: Int

        val duration: Duration

        data class Success(
            override val exitCode: Int,
            override val duration: Duration,
            override val timestamp: Timestamp = Timestamp.now()
        ) : CompletionOutput {
            override val id: String = "completion-success-${UUID.randomUUID()}"

            override val message: String = "Build completed successfully in ${duration.inWholeSeconds}s"
        }

        data class Failure(
            override val exitCode: Int,
            override val duration: Duration,
            val error: String,
            override val timestamp: Timestamp = Timestamp.now()
        ) : CompletionOutput {
            override val id: String = "completion-failure-${UUID.randomUUID()}"

            override val message: String = "Build failed: $error (exit code: $exitCode)"
        }
    }
}