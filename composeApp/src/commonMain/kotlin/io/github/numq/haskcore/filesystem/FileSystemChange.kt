package io.github.numq.haskcore.filesystem

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed interface FileSystemChange {
    val path: String

    val parentPath: String?

    val timestamp: Instant

    data class Created(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }

    data class Modified(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }

    data class Deleted(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }
}