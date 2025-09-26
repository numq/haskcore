package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.clipboard.ClipboardService
import io.github.numq.haskcore.filesystem.FileSystemChange
import io.github.numq.haskcore.filesystem.FileSystemService
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface ExplorerRepository {
    suspend fun initialize(rootPath: String): Result<ExplorerNode.Directory>

    suspend fun getNodes(rootNode: ExplorerNode.Directory): Result<Flow<List<ExplorerNode>>>

    suspend fun createFile(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun createDirectory(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun expandDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun collapseDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun renameNode(node: ExplorerNode, name: String): Result<Unit>

    suspend fun cutNodes(nodes: List<ExplorerNode>): Result<Unit>

    suspend fun copyNodes(nodes: List<ExplorerNode>): Result<Unit>

    suspend fun pasteNodes(destination: ExplorerNode): Result<Unit>

    suspend fun moveNodes(nodes: List<ExplorerNode>, destination: ExplorerNode): Result<Unit>

    suspend fun deleteNodes(nodes: List<ExplorerNode>): Result<Unit>

    class Default(
        private val clipboardService: ClipboardService, private val fileSystemService: FileSystemService
    ) : ExplorerRepository {
        private val nodes = ConcurrentHashMap<String, List<ExplorerNode>>()

        private val operations = MutableSharedFlow<ExplorerOperation>()

        private suspend fun buildNode(rootPath: String, path: String) = Path(path).runCatching {
            val parentPath = parent?.absolutePathString() ?: rootPath

            val depth = when (path) {
                rootPath -> 0

                else -> Path(rootPath).relativize(Path(path)).nameCount
            }

            val lastModified = getLastModifiedTime().toMillis()

            when {
                fileSystemService.isFile(path).getOrThrow() -> ExplorerNode.File(
                    name = name,
                    path = path,
                    parentPath = parentPath,
                    depth = depth,
                    lastModified = lastModified,
                    cut = false,
                    extension = extension
                )

                fileSystemService.isDirectory(path).getOrThrow() -> ExplorerNode.Directory(
                    name = name,
                    path = path,
                    parentPath = parentPath,
                    depth = depth,
                    lastModified = lastModified,
                    cut = false,
                    expanded = nodes.containsKey(path)
                )

                else -> null
            }
        }.getOrNull()

        private fun buildFlattened(rootPath: String, nodes: Map<String, List<ExplorerNode>>) = buildList {
            fun visit(path: String) {
                val children = nodes[path].orEmpty().sortedWith(ExplorerNodeComparator)

                children.forEach { child ->
                    add(child)

                    if (child is ExplorerNode.Directory && child.expanded) {
                        visit(child.path)
                    }
                }
            }

            visit(path = rootPath)
        }

        private suspend fun handleChange(rootPath: String, change: FileSystemChange) {
            val parentPath = when (change) {
                is FileSystemChange.Created -> change.parentPath

                is FileSystemChange.Modified -> change.parentPath

                is FileSystemChange.Deleted -> change.parentPath
            } ?: rootPath

            when (change) {
                is FileSystemChange.Created -> {
                    val createdNode = buildNode(rootPath = rootPath, path = change.path)

                    if (createdNode == null) return

                    nodes.compute(parentPath) { _, value ->
                        val currentChildren = value.orEmpty()

                        when {
                            currentChildren.any { node -> node.path == createdNode.path } -> currentChildren.map { node ->
                                when (node.path) {
                                    createdNode.path -> createdNode

                                    else -> node
                                }
                            }

                            else -> currentChildren + createdNode
                        }
                    }

                    if (createdNode is ExplorerNode.Directory) {
                        val parentNode = buildNode(rootPath = rootPath, path = parentPath) as? ExplorerNode.Directory

                        if (parentNode != null && !parentNode.expanded) {
                            handleExpand(rootPath = rootPath, directory = parentNode.copy(expanded = true))
                        }
                    }
                }

                is FileSystemChange.Modified -> {
                    val modifiedNode = buildNode(rootPath = rootPath, path = change.path)

                    if (modifiedNode == null) return

                    nodes.computeIfPresent(parentPath) { key, value ->
                        value.map { node ->
                            when (node.path) {
                                change.path -> modifiedNode

                                else -> node
                            }
                        }
                    }
                }

                is FileSystemChange.Deleted -> {
                    nodes.computeIfPresent(parentPath) { key, value ->
                        value.filterNot { node ->
                            node.path == change.path
                        }
                    }

                    nodes.keys.removeAll { key -> key.startsWith(change.path) }
                }
            }
        }

        private suspend fun handleExpand(rootPath: String, directory: ExplorerNode.Directory) {
            val path = directory.path

            val parentPath = directory.parentPath

            nodes.computeIfPresent(parentPath) { key, value ->
                value.map { node ->
                    when (node.path) {
                        path -> directory.copy(expanded = true)

                        else -> node
                    }
                }
            }

            val paths = fileSystemService.listDirectory(path = path).getOrThrow()

            val children = paths.mapNotNull { path ->
                buildNode(rootPath = rootPath, path = path)
            }

            nodes[path] = children
        }

        private fun handleCollapse(directory: ExplorerNode.Directory) {
            val path = directory.path

            val parentPath = directory.parentPath

            nodes.computeIfPresent(parentPath) { key, value ->
                value.map { node ->
                    when (node.path) {
                        path -> directory.copy(expanded = false)

                        else -> node
                    }
                }
            }

            nodes.keys.removeAll { key -> key.startsWith(path) }
        }

        private suspend fun handleOperation(rootPath: String, operation: ExplorerOperation) = with(operation) {
            when (this) {
                is ExplorerOperation.Change -> handleChange(rootPath = rootPath, change = change)

                is ExplorerOperation.Expand -> handleExpand(rootPath = rootPath, directory = directory)

                is ExplorerOperation.Collapse -> handleCollapse(directory = directory)
            }
        }

        override suspend fun initialize(rootPath: String) = runCatching {
            require(rootPath.isNotBlank()) { "Root path cannot be blank" }

            val rootNode = checkNotNull(
                buildNode(
                    rootPath = rootPath,
                    path = rootPath
                ) as? ExplorerNode.Directory
            ) { "Failed to build root node" }

            nodes[rootNode.parentPath] = listOf(rootNode)

            val children = fileSystemService.listDirectory(path = rootNode.path).getOrThrow().mapNotNull { path ->
                buildNode(rootPath = rootPath, path = path)
            }

            nodes[rootNode.path] = children

            rootNode
        }

        override suspend fun getNodes(rootNode: ExplorerNode.Directory) = runCatching {
            val rootPath = rootNode.path

            val changes = fileSystemService.observeDirectoryChanges(path = rootNode.path).getOrThrow()

            merge(
                operations, changes.map(ExplorerOperation::Change)
            ).onEach { operation ->
                handleOperation(rootPath = rootPath, operation = operation)
            }.map {
                buildFlattened(rootPath = rootPath, nodes = nodes)
            }.onStart {
                emit(buildFlattened(rootPath = rootPath, nodes = nodes))
            }.catch { println(it) }.distinctUntilChanged()
        }

        override suspend fun expandDirectory(directory: ExplorerNode.Directory) = runCatching {
            operations.emit(ExplorerOperation.Expand(directory))
        }

        override suspend fun collapseDirectory(directory: ExplorerNode.Directory) = runCatching {
            operations.emit(ExplorerOperation.Collapse(directory))
        }

        override suspend fun createFile(destination: ExplorerNode, name: String) = runCatching {
            require(name.isNotBlank()) { "File name cannot be blank" }

            require(!name.contains("/")) { "File name cannot contain path separators" }

            val destinationPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            val path = Path(destinationPath, name).absolutePathString()

            fileSystemService.createFile(path = path, bytes = byteArrayOf()).getOrThrow()
        }

        override suspend fun createDirectory(destination: ExplorerNode, name: String) = runCatching {
            require(name.isNotBlank()) { "Directory name cannot be blank" }

            require(!name.contains("/")) { "Directory name cannot contain path separators" }

            val destinationPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            val path = Path(destinationPath, name).absolutePathString()

            fileSystemService.createDirectory(path = path).getOrThrow()
        }

        override suspend fun renameNode(node: ExplorerNode, name: String) = runCatching {
            require(name.isNotBlank()) { "Name cannot be blank" }

            require(!name.contains("/")) { "Name cannot contain path separators" }

            fileSystemService.rename(path = node.path, name = name).getOrThrow()
        }

        override suspend fun cutNodes(nodes: List<ExplorerNode>) = runCatching {
            clipboardService.cut(paths = nodes.map(ExplorerNode::path)).getOrThrow()
        }

        override suspend fun copyNodes(nodes: List<ExplorerNode>) = runCatching {
            clipboardService.copy(paths = nodes.map(ExplorerNode::path)).getOrThrow()
        }

        override suspend fun pasteNodes(destination: ExplorerNode) = runCatching {
            val destinationPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            clipboardService.paste(path = destinationPath).getOrThrow()
        }

        override suspend fun moveNodes(nodes: List<ExplorerNode>, destination: ExplorerNode) = runCatching {
            val destinationPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            nodes.forEach { node ->
                val fromPath = node.path

                val toPath = Path(destinationPath, node.name).absolutePathString()

                fileSystemService.move(fromPath = fromPath, toPath = toPath, overwrite = false).getOrThrow()
            }
        }

        override suspend fun deleteNodes(nodes: List<ExplorerNode>) = runCatching {
            nodes.forEach { node ->
                fileSystemService.delete(path = node.path).getOrThrow()
            }
        }
    }
}