package io.github.numq.haskcore.workspace

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Workspace {
    @Serializable
    data object None : Workspace

    @Serializable
    data object Loading : Workspace

    @Serializable
    data class Loaded(
        val path: String,
        val name: String,
        val createdAt: Timestamp = Timestamp.now(),
        val updatedAt: Timestamp = createdAt,
        val openedAt: Timestamp = createdAt,
    ) : Workspace
}