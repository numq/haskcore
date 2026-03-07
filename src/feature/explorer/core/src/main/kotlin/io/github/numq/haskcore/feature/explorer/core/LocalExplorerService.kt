package io.github.numq.haskcore.feature.explorer.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

internal class LocalExplorerService(
    private val scope: CoroutineScope, private val explorerDataSource: ExplorerDataSource
) : ExplorerService {
    private val separator = File.separator

    override val explorer = explorerDataSource.explorerData.map(ExplorerData::toExplorer).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Explorer()
    )

    override suspend fun toggleNode(path: String) = explorerDataSource.update { explorerData ->
        val normalized = path.removeSuffix(separator)

        val childPrefix = "$normalized$separator"

        val isExpanded = explorerData.expandedPaths.contains(normalized)

        val expandedPaths = when {
            isExpanded -> explorerData.expandedPaths.filterNot { expandedPath ->
                expandedPath == normalized || expandedPath.startsWith(childPrefix)
            }

            else -> explorerData.expandedPaths + normalized
        }

        explorerData.copy(expandedPaths = expandedPaths, selectedPath = normalized)
    }.map {}

    override suspend fun selectNode(path: String) = explorerDataSource.update { explorerData ->
        explorerData.copy(selectedPath = path)
    }.map {}

    override suspend fun saveExplorerPosition(position: ExplorerPosition) = explorerDataSource.update { explorerData ->
        with(position) {
            explorerData.copy(index = index, offset = offset)
        }
    }.map {}
}