package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.*
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class ObserveExplorerTree(
    private val root: ExplorerRoot, private val explorerService: ExplorerService, private val vfsService: VfsService
) : UseCase<Unit, Flow<ExplorerTree>> {
    private suspend fun buildTree(cache: Map<String, List<VirtualFile>>, explorer: Explorer): ExplorerTree {
        val rootPath = root.path

        val expandedPath = explorer.expandedPaths.toSet()

        val isRootExpanded = expandedPath.contains(rootPath)

        val nodes = buildList {
            add(
                ExplorerNode.Directory(
                    name = explorerService.getName(path = rootPath).getOrElse { "" }.ifEmpty { rootPath },
                    path = rootPath,
                    level = 0,
                    segments = emptyList(),
                    isExpanded = isRootExpanded
                )
            )

            fun fill(path: String, level: Int, parentSegments: List<String>) {
                val segments = parentSegments + path

                val files = cache[path]?.filterNot { file ->
                    val isMetaDir = file.isDirectory && file.name == ".haskcore"

                    val isMetaFile = !file.isDirectory && (file.name.endsWith(".pb") || file.name.endsWith(".tmp"))

                    isMetaDir || isMetaFile
                }?.sortedWith(compareByDescending(VirtualFile::isDirectory).thenBy(VirtualFile::path)) ?: return

                files.forEach { file ->
                    val isExpanded = expandedPath.contains(file.path)

                    add(
                        when {
                            file.isDirectory -> ExplorerNode.Directory(
                                name = file.name,
                                path = file.path,
                                level = level,
                                segments = segments,
                                isExpanded = isExpanded
                            )

                            else -> ExplorerNode.File(
                                name = file.name,
                                path = file.path,
                                level = level,
                                segments = segments,
                                extension = file.extension
                            )
                        }
                    )

                    if (file.isDirectory && isExpanded) {
                        fill(path = file.path, level = level + 1, parentSegments = segments)
                    }
                }
            }

            if (isRootExpanded) {
                fill(path = rootPath, level = 1, parentSegments = emptyList())
            }
        }

        return when {
            isRootExpanded && nodes.size == 1 && !cache.containsKey(rootPath) -> ExplorerTree.Loading(root = root)

            else -> ExplorerTree.Loaded(
                root = root,
                nodes = nodes,
                position = ExplorerPosition(index = explorer.index, offset = explorer.offset)
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
            vfsService.observeVisibleFiles(path = path).fold(ifLeft = {
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
        buildTree(cache = cache, explorer = data)
    }
}