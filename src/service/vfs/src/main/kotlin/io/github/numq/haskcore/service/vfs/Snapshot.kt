package io.github.numq.haskcore.service.vfs

import io.github.numq.haskcore.common.core.timestamp.Timestamp

internal data class Snapshot(
    val path: String, val timestamp: Timestamp, val files: Map<String, List<VirtualFile>>,
)