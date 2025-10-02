package io.github.numq.haskcore.filesystem

import io.github.numq.haskcore.timestamp.Timestamp

internal sealed interface FileSystemChange {
    val path: String

    val parentPath: String?

    val timestamp: Timestamp

    data class Created(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Timestamp.now()
    }

    data class Modified(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Timestamp.now()
    }

    data class Deleted(override val path: String, override val parentPath: String?) : FileSystemChange {
        override val timestamp = Timestamp.now()
    }
}