package io.github.numq.haskcore.session

import kotlinx.serialization.Serializable

@Serializable
internal data class Session(
    val workspacePath: String? = null, val recentWorkspaces: List<RecentWorkspace> = emptyList()
)