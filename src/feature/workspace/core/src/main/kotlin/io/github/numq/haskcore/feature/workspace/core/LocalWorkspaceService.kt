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
    private val scope: CoroutineScope, private val workspaceDataSource: WorkspaceDataSource,
) : WorkspaceService {
    override val workspace = workspaceDataSource.workspaceData.map(WorkspaceData::toWorkspace).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Workspace()
    )

    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

    override suspend fun selectShelfTool(tool: ShelfTool) = workspaceDataSource.update { workspaceData ->
        val toolData = tool.toShelfToolData()

        val shelfData = workspaceData.shelfData ?: ShelfData()

        fun updatePanel(panel: ShelfPanelData) = when (toolData) {
            in panel.tools -> panel.copy(
                activeTool = when (panel.activeTool) {
                    toolData -> null

                    else -> toolData
                }
            )

            else -> panel
        }

        workspaceData.copy(
            shelfData = shelfData.copy(
                leftPanel = updatePanel(panel = shelfData.leftPanel),
                rightPanel = updatePanel(panel = shelfData.rightPanel)
            )
        )
    }.map {}

    override suspend fun saveLeftShelfPanelRatio(ratio: Float) = workspaceDataSource.update { workspaceData ->
        val shelfData = workspaceData.shelfData ?: ShelfData()

        workspaceData.copy(shelfData = shelfData.copy(leftPanel = shelfData.leftPanel.copy(ratio = ratio)))
    }.map {}

    override suspend fun saveRightShelfPanelRatio(ratio: Float) = workspaceDataSource.update { workspaceData ->
        val shelfData = workspaceData.shelfData ?: ShelfData()

        workspaceData.copy(shelfData = shelfData.copy(rightPanel = shelfData.rightPanel.copy(ratio = ratio)))
    }.map {}

    override suspend fun saveVerticalRatio(ratio: Float) = workspaceDataSource.update { workspaceData ->
        workspaceData.copy(verticalRatio = ratio)
    }.map {}

    override suspend fun saveDimensions(
        x: Float, y: Float, width: Float, height: Float, isFullscreen: Boolean,
    ) = workspaceDataSource.update { workspaceData ->
        workspaceData.copy(x = x, y = y, width = width, height = height, isFullscreen = isFullscreen)
    }.map {}

    override fun close() {
        scope.cancel()
    }
}