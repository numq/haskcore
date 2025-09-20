package io.github.numq.haskcore.filesystem

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed interface FileSystemNode {
    val path: String

    val name: String

    val size: Long

    val isHidden: Boolean

    val isReadOnly: Boolean

    val permissions: String

    val createdAt: Instant

    val accessedAt: Instant

    val modifiedAt: Instant

    val parent: FileSystemNode?

    data class File(
        override val path: String,
        override val name: String,
        override val size: Long,
        override val isHidden: Boolean,
        override val isReadOnly: Boolean,
        override val permissions: String,
        override val createdAt: Instant,
        override val accessedAt: Instant,
        override val modifiedAt: Instant,
        override val parent: FileSystemNode?
    ) : FileSystemNode {
        val extension: String get() = name.substringAfterLast('.', "")
    }

    data class Directory(
        override val name: String,
        override val path: String,
        override val size: Long,
        override val isHidden: Boolean,
        override val isReadOnly: Boolean,
        override val permissions: String,
        override val createdAt: Instant,
        override val accessedAt: Instant,
        override val modifiedAt: Instant,
        override val parent: FileSystemNode?,
        val children: List<FileSystemNode>,
    ) : FileSystemNode {
        fun findFile(name: String) = children.filterIsInstance<File>().firstOrNull { file ->
            file.name == name
        }

        fun findDirectory(name: String) = children.filterIsInstance<Directory>().firstOrNull { directory ->
            directory.name == name
        }
    }
}