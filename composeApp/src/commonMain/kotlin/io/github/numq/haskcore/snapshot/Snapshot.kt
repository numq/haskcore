package io.github.numq.haskcore.snapshot

import io.github.numq.haskcore.workspace.Workspace

internal data class Snapshot(
    val lastOpenedWorkspacePath: String? = null,
    val recentWorkspaces: List<Workspace> = emptyList(),
)