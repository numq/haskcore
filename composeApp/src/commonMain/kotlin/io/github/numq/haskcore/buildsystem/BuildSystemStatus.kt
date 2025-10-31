package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp

internal sealed interface BuildSystemStatus {
    val timestamp: Timestamp

    data class OutOfSync(override val timestamp: Timestamp = Timestamp.now()) : BuildSystemStatus

    data class Syncing(override val timestamp: Timestamp = Timestamp.now()) : BuildSystemStatus

    sealed interface Synced : BuildSystemStatus {
        val system: BuildSystem

        val artifacts: List<BuildSystemArtifact>

        data class Idle(
            override val system: BuildSystem,
            override val artifacts: List<BuildSystemArtifact>,
            override val timestamp: Timestamp = Timestamp.now()
        ) : Synced

        sealed interface Active : Synced {
            val currentArtifact: BuildSystemArtifact

            val progress: BuildProgress

            data class Building(
                override val system: BuildSystem,
                override val artifacts: List<BuildSystemArtifact>,
                override val currentArtifact: BuildSystemArtifact,
                override val progress: BuildProgress = BuildProgress.Active,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Testing(
                override val system: BuildSystem,
                override val artifacts: List<BuildSystemArtifact>,
                override val currentArtifact: BuildSystemArtifact,
                override val progress: BuildProgress = BuildProgress.Active,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Running(
                override val system: BuildSystem,
                override val artifacts: List<BuildSystemArtifact>,
                override val currentArtifact: BuildSystemArtifact,
                override val progress: BuildProgress = BuildProgress.Active,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Compiling(
                override val system: BuildSystem,
                override val artifacts: List<BuildSystemArtifact>,
                override val currentArtifact: BuildSystemArtifact,
                override val progress: BuildProgress = BuildProgress.Active,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active
        }
    }

    data class Error(val throwable: Throwable, override val timestamp: Timestamp = Timestamp.now()) : BuildSystemStatus
}