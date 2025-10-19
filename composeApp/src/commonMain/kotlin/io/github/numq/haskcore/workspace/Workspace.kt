package io.github.numq.haskcore.workspace

import io.github.numq.haskcore.stack.StackProject
import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable

@Serializable
internal data class Workspace(
    val path: String,
    val name: String,
    val stackProject: StackProject? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = createdAt,
)