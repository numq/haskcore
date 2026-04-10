package io.github.numq.haskcore.service.vfs

import io.github.numq.haskcore.core.timestamp.Timestamp

data class VirtualFile(
    val path: String,
    val name: String,
    val nameWithoutExtension: String,
    val extension: String?,
    val isDirectory: Boolean,
    val isMetadata: Boolean,
    val size: Long,
    val lastModifiedTimestamp: Timestamp
)