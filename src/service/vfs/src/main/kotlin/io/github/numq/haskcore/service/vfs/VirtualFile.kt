package io.github.numq.haskcore.service.vfs

import io.github.numq.haskcore.core.timestamp.Timestamp

data class VirtualFile(
    val path: String,
    val name: String,
    val extension: String?,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Timestamp
)