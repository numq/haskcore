package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.time.Duration

@Serializable
internal sealed interface BuildOutput {
    val id: String

    val target: BuildTarget

    val message: String

    val timestamp: Timestamp

    data class Progress(
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "progress-${UUID.randomUUID()}"
    }

    data class BuildModule(
        val module: String,
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now(),
    ) : BuildOutput {
        override val id: String = "build-$module-${UUID.randomUUID()}"
    }

    data class TestResult(
        val module: String,
        val passed: Boolean,
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now(),
    ) : BuildOutput {
        override val id: String = "test-$module-${UUID.randomUUID()}"
    }

    data class Warning(
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "warning-${UUID.randomUUID()}"
    }

    data class Error(
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "error-${UUID.randomUUID()}"
    }

    data class Run(
        override val target: BuildTarget,
        override val message: String,
        override val timestamp: Timestamp = Timestamp.now()
    ) : BuildOutput {
        override val id: String = "run-${UUID.randomUUID()}"
    }

    sealed interface Completion : BuildOutput {
        val exitCode: Int

        val duration: Duration

        data class Success(
            override val exitCode: Int,
            override val duration: Duration,
            override val target: BuildTarget,
            override val timestamp: Timestamp = Timestamp.now(),
        ) : Completion {
            override val id: String = "completion-success-${UUID.randomUUID()}"

            override val message: String = "Build completed successfully in ${duration.inWholeSeconds}s"
        }

        data class Failure(
            val error: String,
            override val exitCode: Int,
            override val duration: Duration,
            override val target: BuildTarget,
            override val timestamp: Timestamp = Timestamp.now()
        ) : Completion {
            override val id: String = "completion-failure-${UUID.randomUUID()}"

            override val message: String = "Build failed: $error (exit code: $exitCode)"
        }
    }
}