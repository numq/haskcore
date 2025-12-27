package io.github.numq.haskcore.buildsystem

import io.github.numq.haskcore.timestamp.Timestamp

internal sealed interface BuildStatus {
    val timestamp: Timestamp

    data object OutOfSynchronization : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data object Synchronizing : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data class Synchronized(val path: String, val targets: List<BuildTarget>) : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data class Error(val throwable: Throwable) : BuildStatus {
        override val timestamp = Timestamp.now()
    }
}