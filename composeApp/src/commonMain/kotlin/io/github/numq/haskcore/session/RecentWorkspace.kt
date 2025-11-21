package io.github.numq.haskcore.session

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable

@Serializable
internal data class RecentWorkspace(val path: String, val name: String, val timestamp: Timestamp)