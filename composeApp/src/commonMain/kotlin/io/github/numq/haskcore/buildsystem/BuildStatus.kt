package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp

internal sealed interface BuildStatus {
    val timestamp: Timestamp

    data class OutOfSync(override val timestamp: Timestamp = Timestamp.now()) : BuildStatus

    data class Syncing(override val timestamp: Timestamp = Timestamp.now()) : BuildStatus

    sealed interface Synced : BuildStatus {
        val system: BuildSystem

        val targets: List<BuildTarget>

        data class Idle(
            override val system: BuildSystem,
            override val targets: List<BuildTarget>,
            override val timestamp: Timestamp = Timestamp.now()
        ) : Synced

        sealed interface Active : Synced {
            val currentTarget: BuildTarget

            data class Building(
                override val system: BuildSystem,
                override val targets: List<BuildTarget>,
                override val currentTarget: BuildTarget,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Testing(
                override val system: BuildSystem,
                override val targets: List<BuildTarget>,
                override val currentTarget: BuildTarget,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Running(
                override val system: BuildSystem,
                override val targets: List<BuildTarget>,
                override val currentTarget: BuildTarget,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active

            data class Compiling(
                override val system: BuildSystem,
                override val targets: List<BuildTarget>,
                override val currentTarget: BuildTarget,
                override val timestamp: Timestamp = Timestamp.now()
            ) : Active
        }
    }

    data class Error(val throwable: Throwable, override val timestamp: Timestamp = Timestamp.now()) : BuildStatus
}