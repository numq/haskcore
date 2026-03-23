package io.github.numq.haskcore.feature.explorer.core

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name

internal class LocalExplorerService(
    private val scope: CoroutineScope, private val explorerDataSource: ExplorerDataSource
) : ExplorerService {
    private val separator = File.separator

    override val explorer = explorerDataSource.explorerData.map(ExplorerData::toExplorer).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Explorer()
    )

    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

    override suspend fun collapseDirectory(node: ExplorerNode.Directory) = explorerDataSource.update { explorerData ->
        val path = node.path.removeSuffix(separator)

        val expandedPaths = explorerData.expandedPaths.filterNot { expandedPath ->
            expandedPath == path || expandedPath.startsWith(path + separator)
        }

        explorerData.copy(expandedPaths = expandedPaths)
    }.map {}

    override suspend fun expandDirectory(node: ExplorerNode.Directory) = explorerDataSource.update { explorerData ->
        val path = node.path.removeSuffix(separator)

        when {
            path in explorerData.expandedPaths -> explorerData

            else -> explorerData.copy(expandedPaths = explorerData.expandedPaths + path)
        }
    }.map {}

    override suspend fun saveExplorerPosition(position: ExplorerPosition) = explorerDataSource.update { explorerData ->
        with(position) {
            explorerData.copy(index = index, offset = offset)
        }
    }.map {}
}