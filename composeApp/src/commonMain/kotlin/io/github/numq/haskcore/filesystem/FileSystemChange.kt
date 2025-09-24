@file:OptIn(ExperimentalTime::class)

package io.github.numq.haskcore.filesystem

import java.nio.file.Path
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface FileSystemChange {
    val path: Path

    val timestamp: Instant

    data class Created(override val path: Path) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }

    data class Modified(override val path: Path) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }

    data class Deleted(override val path: Path) : FileSystemChange {
        override val timestamp = Clock.System.now()
    }
}