package io.github.numq.haskcore.filesystem

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed interface FileSystemEvent {
    val path: String

    val timestamp: Instant

    data class Created(override val path: String) : FileSystemEvent {
        override val timestamp = Clock.System.now()
    }

    data class Modified(override val path: String) : FileSystemEvent {
        override val timestamp = Clock.System.now()
    }

    data class Deleted(override val path: String) : FileSystemEvent {
        override val timestamp = Clock.System.now()
    }
}