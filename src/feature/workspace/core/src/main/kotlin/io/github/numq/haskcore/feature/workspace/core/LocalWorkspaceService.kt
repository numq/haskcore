package io.github.numq.haskcore.feature.workspace.core

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.name

internal class LocalWorkspaceService(
    private val scope: CoroutineScope, private val workspaceDataSource: WorkspaceDataSource
) : WorkspaceService {
    override val workspace = workspaceDataSource.workspaceData.map(WorkspaceData::toWorkspace).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Workspace()
    )

    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

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