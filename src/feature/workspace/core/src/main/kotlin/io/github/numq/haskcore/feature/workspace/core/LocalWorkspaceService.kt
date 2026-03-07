package io.github.numq.haskcore.feature.workspace.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalWorkspaceService(
    private val scope: CoroutineScope, private val workspaceDataSource: WorkspaceDataSource
) : WorkspaceService {
    override val workspace = workspaceDataSource.workspaceData.map(WorkspaceData::toWorkspace).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Workspace()
    )

    override suspend fun saveDimensions(
        x: Float, y: Float, width: Float, height: Float, isFullscreen: Boolean
    ) = workspaceDataSource.update { workspaceData ->
        workspaceData.copy(x = x, y = y, width = width, height = height, isFullscreen = isFullscreen)
    }.map {}

    override suspend fun saveRatio(ratio: Float) = workspaceDataSource.update { workspaceData ->
        workspaceData.copy(ratio = ratio)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}