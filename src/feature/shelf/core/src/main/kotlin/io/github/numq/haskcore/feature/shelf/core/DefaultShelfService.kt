package io.github.numq.haskcore.feature.shelf.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DefaultShelfService(
    private val scope: CoroutineScope, private val shelfDataSource: ShelfDataSource
) : ShelfService {
    override val shelf = shelfDataSource.shelfData.map(ShelfData::toShelf).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Shelf()
    )

    override suspend fun updateLeftRatio(ratio: Float) = shelfDataSource.update { shelfData ->
        shelfData.copy(leftPanel = shelfData.leftPanel.copy(ratio = ratio))
    }.map {}

    override suspend fun updateRightRatio(ratio: Float) = shelfDataSource.update { shelfData ->
        shelfData.copy(rightPanel = shelfData.rightPanel.copy(ratio = ratio))
    }.map {}

    override suspend fun selectShelfTool(tool: ShelfTool) = shelfDataSource.update { shelfData ->
        val toolData = tool.toShelfToolData()

        fun updatePanel(panel: ShelfPanelData) = when (toolData) {
            in panel.tools -> panel.copy(
                activeTool = when (panel.activeTool) {
                    toolData -> null

                    else -> toolData
                }
            )

            else -> panel
        }

        shelfData.copy(
            leftPanel = updatePanel(panel = shelfData.leftPanel), rightPanel = updatePanel(panel = shelfData.rightPanel)
        )
    }.map {}

    override fun close() {
        scope.cancel()
    }
}