package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.*
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File

class ObserveExplorerTree(
    private val root: ExplorerRoot, private val explorerService: ExplorerService, private val vfsService: VfsService
) : UseCase<Unit, Flow<ExplorerTree>> {
    private fun buildTree(cache: Map<String, List<VirtualFile>>, explorer: Explorer): ExplorerTree {
        val rootPath = root.path

        val expandedPath = explorer.expandedPaths.toSet()

        val selectedPath = explorer.selectedPath ?: rootPath

        val isRootExpanded = expandedPath.contains(rootPath)

        val nodes = mutableListOf<ExplorerNode>()

        nodes.add(
            ExplorerNode.Directory(
                name = File(rootPath).name.ifEmpty { rootPath },
                path = rootPath,
                level = 0,
                isSelected = rootPath == selectedPath,
                isExpanded = isRootExpanded
            )
        )

        fun fill(path: String, level: Int) {
            val files = cache[path]?.filterNot { file ->
                val isMetaDir = file.isDirectory && file.name == ".haskcore"

                val isMetaFile = !file.isDirectory && (file.name.endsWith(".pb") || file.name.endsWith(".tmp"))

                isMetaDir || isMetaFile
            }?.sortedWith(compareByDescending(VirtualFile::isDirectory).thenBy(VirtualFile::path)) ?: return

            files.forEach { file ->
                val isExpanded = expandedPath.contains(file.path)

                nodes.add(
                    when {
                        file.isDirectory -> ExplorerNode.Directory(
                            name = file.name,
                            path = file.path,
                            level,
                            isSelected = file.path == selectedPath,
                            isExpanded = isExpanded
                        )

                        else -> ExplorerNode.File(
                            name = file.name,
                            path = file.path,
                            level = level,
                            isSelected = file.path == selectedPath,
                            extension = file.extension
                        )
                    }
                )

                if (file.isDirectory && isExpanded) {
                    fill(path = file.path, level = level + 1)
                }
            }
        }

        if (isRootExpanded) {
            fill(path = rootPath, level = 1)
        }

        return when {
            isRootExpanded && nodes.size == 1 && !cache.containsKey(rootPath) -> ExplorerTree.Loading(root = root)

            else -> ExplorerTree.Loaded(
                root = root,
                position = ExplorerPosition(index = explorer.index, offset = explorer.offset),
                selectedPath = selectedPath,
                nodes = nodes
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun Raise<Throwable>.execute(input: Unit) = explorerService.explorer.map { explorer ->
        explorer.expandedPaths.toSet() + root.path
    }.distinctUntilChanged().scan(emptyMap<String, Flow<Pair<String, List<VirtualFile>>>>()) { acc, paths ->
        val current = acc.filterKeys(paths::contains)

        val newPaths = paths - current.keys

        current + newPaths.associateWith { path ->
            vfsService.observeDirectory(path).fold(ifLeft = {
                flowOf(path to emptyList())
            }, ifRight = { flow ->
                flow.map { files ->
                    path to files
                }
            })
        }
    }.flatMapLatest { observers ->
        when {
            observers.isEmpty() -> flowOf(mapOf(root.path to emptyList()))

            else -> combine(flows = observers.values, transform = Array<Pair<String, List<VirtualFile>>>::toMap)
        }
    }.combine(explorerService.explorer) { cache, data ->
        buildTree(cache, data)
    }
}