package io.github.numq.haskcore.buildtool

import io.github.numq.haskcore.timestamp.Timestamp

internal interface BuildStatus {
    val timestamp: Timestamp

    data object OutOfSynchronization : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data object Synchronizing : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data class Synchronized(val packages: List<BuildPackage>) : BuildStatus {
        override val timestamp = Timestamp.now()
    }

    data class Error(val exception: Exception) : BuildStatus {
        override val timestamp = Timestamp.now()
    }
}