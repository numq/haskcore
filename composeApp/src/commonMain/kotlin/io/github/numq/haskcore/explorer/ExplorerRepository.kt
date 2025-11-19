package io.github.numq.haskcore.explorer

import androidx.datastore.core.Closeable
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

internal interface ExplorerRepository : Closeable {
    val explorer: Flow<Explorer>

    suspend fun createFile(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun createDirectory(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun expandDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun collapseDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun renameNode(node: ExplorerNode, name: String): Result<Unit>

    suspend fun copyNodes(nodes: Set<ExplorerNode>, destination: ExplorerNode, overwrite: Boolean): Result<Unit>

    suspend fun moveNodes(nodes: Set<ExplorerNode>, destination: ExplorerNode, overwrite: Boolean): Result<Unit>

    suspend fun deleteNodes(nodes: Set<ExplorerNode>): Result<Unit>

    class Default(
        private val rootPath: String, private val explorerSnapshotDataSource: ExplorerSnapshotDataSource
    ) : ExplorerRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private val _explorerTree = MutableStateFlow<ExplorerTree?>(null)

        private val changeEvents = callbackFlow {
            val watcher = DirectoryWatcher.builder().path(Path.of(rootPath)).listener(::trySend).build()

            val future = watcher.watchAsync()

            awaitClose {
                future.cancel(true)

                watcher.close()
            }
        }.buffer(Channel.UNLIMITED).distinctUntilChanged().flowOn(Dispatchers.IO)

        override val explorer = flow {
            emit(Explorer.Loading)

            _explorerTree.collect { tree ->
                if (tree != null) {
                    emit(Explorer.Loaded(rootPath = rootPath, nodes = tree.flatten()))
                }
            }
        }

        init {
            coroutineScope.launch {
                val validatedSnapshot = explorerSnapshotDataSource.update { snapshot ->
                    val expandedDirectoryPaths = snapshot.expandedDirectoryPaths.filter { path ->
                        File(path).isDirectory
                    }

                    snapshot.copy(expandedDirectoryPaths = expandedDirectoryPaths)
                }.getOrThrow()

                _explorerTree.value = buildInitialTree().applySnapshot(validatedSnapshot)

                changeEvents.collect { event ->
                    _explorerTree.updateAndGet { tree ->
                        tree?.let { tree ->
                            handleChange(tree = tree, event = event)
                        }
                    }
                }
            }
        }

        private fun checkFile(path: Path) {
            if (!path.isRegularFile()) {
                throw ExplorerException("Path '${path.absolutePathString()}' is not a regular file")
            }
        }

        private fun checkDirectory(path: Path) {
            if (!path.isDirectory()) {
                throw ExplorerException("Path '${path.absolutePathString()}' is not a directory")
            }
        }

        private fun checkPathExists(path: Path) {
            if (path.notExists()) {
                throw ExplorerException("Path '${path.absolutePathString()}' does not exist")
            }
        }

        private fun checkPathNotExists(path: Path, overwrite: Boolean) {
            if (path.exists() && !overwrite) {
                throw ExplorerException("Path '${path.absolutePathString()}' already exists")
            }
        }

        private fun checkTransferPaths(src: Path, dst: Path, overwrite: Boolean) {
            checkPathExists(path = src)

            checkPathNotExists(path = dst, overwrite = overwrite)
        }

        private fun transfer(src: Path, dst: Path, overwrite: Boolean, copy: Boolean) {
            checkTransferPaths(src = src, dst = dst, overwrite = overwrite)

            val options = when {
                overwrite -> arrayOf(StandardCopyOption.REPLACE_EXISTING)

                else -> emptyArray()
            }

            when {
                copy -> Files.copy(src, dst, *options)

                else -> Files.move(src, dst, *options)
            }
        }

        private fun buildFileNode(rootPath: Path, path: Path): ExplorerNode.File {
            checkFile(path = path)

            val depth = when (path) {
                rootPath -> 0

                else -> rootPath.relativize(path).nameCount
            }

            val lastModified = path.getLastModifiedTime().toMillis()

            return with(path) {
                ExplorerNode.File(
                    name = name,
                    path = absolutePathString(),
                    parentPath = parent.absolutePathString(),
                    depth = depth,
                    lastModified = lastModified,
                    extension = extension,
                    nameWithoutExtension = nameWithoutExtension,
                )
            }
        }

        private fun buildDirectoryNode(rootPath: Path, path: Path): ExplorerNode.Directory {
            checkDirectory(path = path)

            val depth = when (path) {
                rootPath -> 0

                else -> rootPath.relativize(path).nameCount
            }

            val lastModified = path.getLastModifiedTime().toMillis()

            return with(path) {
                ExplorerNode.Directory(
                    name = name,
                    path = absolutePathString(),
                    parentPath = parent.absolutePathString(),
                    depth = depth,
                    lastModified = lastModified,
                    expanded = false
                )
            }
        }

        private fun buildNode(rootPath: Path, path: Path) = when {
            path.isRegularFile() -> buildFileNode(rootPath = rootPath, path = path)

            path.isDirectory() -> buildDirectoryNode(rootPath = rootPath, path = path)

            else -> null
        }

        private fun buildChildren(rootPath: Path, path: Path) = path.listDirectoryEntries().mapNotNull { childPath ->
            buildNode(rootPath = rootPath, path = childPath)
        }.toPersistentSet()

        private fun buildInitialTree(): ExplorerTree {
            val rootPath = Path.of(rootPath)

            val rootNode = buildDirectoryNode(rootPath = rootPath, path = rootPath)

            val rootChildren = buildChildren(rootPath = rootPath, path = rootPath)

            val childrenByPath = persistentMapOf(rootPath.absolutePathString() to rootChildren)

            return ExplorerTree(rootNode = rootNode, childrenByPath = childrenByPath)
        }

        private fun handleExpand(tree: ExplorerTree, directory: ExplorerNode.Directory) = when {
            directory.expanded -> tree

            else -> updateNodeInTree(
                tree = tree,
                updatedNode = directory.copy(expanded = true),
                children = buildChildren(rootPath = Path.of(tree.rootNode.path), path = Path.of(directory.path))
            )
        }

        private fun handleCollapse(tree: ExplorerTree, directory: ExplorerNode.Directory) = when {
            !directory.expanded -> tree

            else -> updateNodeInTree(
                tree = tree,
                updatedNode = directory.copy(expanded = false),
                children = tree.childrenByPath[directory.path]
            )
        }

        private fun updateNodeInTree(
            tree: ExplorerTree,
            updatedNode: ExplorerNode.Directory,
            children: PersistentSet<ExplorerNode>? = null,
        ) = when {
            updatedNode.path == tree.rootNode.path -> {
                var newChildrenByPath = tree.childrenByPath

                children?.let { childNode ->
                    newChildrenByPath = newChildrenByPath.put(updatedNode.path, childNode)
                }

                tree.copy(rootNode = updatedNode, childrenByPath = newChildrenByPath)
            }

            else -> {
                val parentPath = updatedNode.parentPath

                val currentChildren = tree.childrenByPath[parentPath] ?: return tree

                val newChildren = currentChildren.map { node ->
                    when (node.path) {
                        updatedNode.path -> updatedNode

                        else -> node
                    }
                }.toPersistentSet()

                var newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

                children?.let { childNode ->
                    newChildrenByPath = newChildrenByPath.put(updatedNode.path, childNode)
                }

                tree.copy(childrenByPath = newChildrenByPath)
            }
        }

        private fun findDirectoryInTree(tree: ExplorerTree, path: String): ExplorerNode.Directory? {
            if (tree.rootNode.path == path) {
                return tree.rootNode
            }

            fun findInChildren(children: PersistentSet<ExplorerNode>): ExplorerNode.Directory? {
                children.forEach { node ->
                    if (node.path == path && node is ExplorerNode.Directory) {
                        return node
                    }

                    if (node is ExplorerNode.Directory && node.expanded) {
                        tree.childrenByPath[node.path]?.let { childResult ->
                            findInChildren(children = childResult)?.let { return it }
                        }
                    }
                }

                return null
            }

            return tree.childrenByPath[tree.rootNode.path]?.let(::findInChildren)
        }

        private fun handleCreate(tree: ExplorerTree, rootPath: Path, path: Path): ExplorerTree {
            val parentPath = path.parent ?: rootPath

            val newNode = buildNode(rootPath = rootPath, path = path) ?: return tree

            val currentChildren = tree.childrenByPath[parentPath.absolutePathString()] ?: emptyList()

            val newChildren = when {
                currentChildren.any { childNode -> childNode.path == path.absolutePathString() } -> currentChildren.map { childNode ->
                    when {
                        childNode.path == path.absolutePathString() -> newNode

                        else -> childNode
                    }
                }

                else -> currentChildren + newNode
            }.toPersistentSet()

            var newTree =
                tree.copy(childrenByPath = tree.childrenByPath.put(parentPath.absolutePathString(), newChildren))

            if (newNode is ExplorerNode.Directory) {
                val parentDirectory = findDirectoryInTree(tree = tree, path = parentPath.absolutePathString())

                if (parentDirectory != null && parentDirectory.expanded) {
                    val expandedNewNode = newNode.copy(expanded = true)

                    val newChildrenForParent = newChildren.map { childNode ->
                        when {
                            childNode.path == path.absolutePathString() -> expandedNewNode

                            else -> childNode
                        }
                    }.toPersistentSet()

                    val childrenOfNewNode = buildChildren(rootPath = rootPath, path = Path.of(expandedNewNode.path))

                    newTree = newTree.copy(
                        childrenByPath = newTree.childrenByPath.put(
                            parentPath.absolutePathString(), newChildrenForParent
                        ).put(expandedNewNode.path, childrenOfNewNode)
                    )
                }
            }

            return newTree
        }

        private fun handleModify(tree: ExplorerTree, rootPath: Path, path: Path): ExplorerTree {
            val parentPath = path.parent.absolutePathString()

            val modifiedNode = buildNode(rootPath = rootPath, path = path) ?: return tree

            val currentChildren = tree.childrenByPath[parentPath] ?: return tree

            val oldNode = currentChildren.find { childNode -> childNode.path == path.absolutePathString() }

            val newChildren = currentChildren.map { node ->
                when {
                    node.path == path.absolutePathString() -> when {
                        modifiedNode is ExplorerNode.Directory -> modifiedNode.copy(expanded = false)

                        else -> modifiedNode
                    }

                    else -> node
                }
            }.toPersistentSet()

            var newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

            if (modifiedNode is ExplorerNode.Directory) {
                newChildrenByPath = newChildrenByPath.remove(path.absolutePathString())

                val wasExpanded = oldNode is ExplorerNode.Directory && oldNode.expanded

                if (wasExpanded) {
                    val pathsToRemove = tree.childrenByPath.keys.filter { pathToRemove ->
                        pathToRemove.startsWith(path.absolutePathString()) && pathToRemove != path.absolutePathString()
                    }

                    pathsToRemove.forEach { path ->
                        newChildrenByPath = newChildrenByPath.remove(path)
                    }
                }
            }

            return tree.copy(childrenByPath = newChildrenByPath)
        }

        private fun handleDelete(tree: ExplorerTree, path: Path): ExplorerTree {
            val parentPath = path.parent.absolutePathString()

            val currentChildren = tree.childrenByPath[parentPath] ?: return tree

            val newChildren = currentChildren.filter { childNode ->
                childNode.path != path.absolutePathString()
            }.toPersistentSet()

            val newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

            return tree.copy(childrenByPath = newChildrenByPath)
        }

        private fun handleOverflow(tree: ExplorerTree) = tree // TODO: handle overflow

        private fun handleChange(tree: ExplorerTree, event: DirectoryChangeEvent) = when (event.eventType()) {
            DirectoryChangeEvent.EventType.CREATE -> handleCreate(
                tree = tree, rootPath = event.rootPath(), path = event.path()
            )

            DirectoryChangeEvent.EventType.MODIFY -> handleModify(
                tree = tree, rootPath = event.rootPath(), path = event.path()
            )

            DirectoryChangeEvent.EventType.DELETE -> handleDelete(tree = tree, path = event.path())

            DirectoryChangeEvent.EventType.OVERFLOW -> handleOverflow(tree = tree)
        }

        override suspend fun expandDirectory(directory: ExplorerNode.Directory) = runCatching {
            _explorerTree.update { tree ->
                tree?.let { tree ->
                    val updatedTree = handleExpand(tree = tree, directory = directory)

                    val expandedPaths = updatedTree.getExpandedPaths().toList()

                    explorerSnapshotDataSource.update { snapshot ->
                        snapshot.copy(expandedDirectoryPaths = expandedPaths)
                    }.getOrThrow()

                    updatedTree
                }
            }
        }

        override suspend fun collapseDirectory(directory: ExplorerNode.Directory) = runCatching {
            _explorerTree.update { tree ->
                tree?.let { tree ->
                    val updatedTree = handleCollapse(tree = tree, directory = directory)

                    val expandedPaths = updatedTree.getExpandedPaths().toList()

                    explorerSnapshotDataSource.update { snapshot ->
                        snapshot.copy(expandedDirectoryPaths = expandedPaths)
                    }.getOrThrow()

                    updatedTree
                }
            }
        }

        override suspend fun createFile(destination: ExplorerNode, name: String) = runCatching {
            val dstPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            val path = Path.of(dstPath).resolve(name)

            checkPathNotExists(path = path, overwrite = false)

            path.createParentDirectories()

            path.createFile()

            checkPathExists(path = path)
        }

        override suspend fun createDirectory(destination: ExplorerNode, name: String) = runCatching {
            val dstPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            val path = Path.of(dstPath).resolve(name)

            checkPathNotExists(path = path, overwrite = false)

            path.createDirectories()

            checkPathExists(path = path)
        }

        override suspend fun renameNode(node: ExplorerNode, name: String) = runCatching {
            val src = Path.of(node.path)

            val dst = src.resolveSibling(name)

            if (Files.isSameFile(src, dst)) {
                return@runCatching
            }

            transfer(src = src, dst = dst, overwrite = false, copy = false)
        }

        override suspend fun copyNodes(
            nodes: Set<ExplorerNode>, destination: ExplorerNode, overwrite: Boolean
        ) = runCatching {
            nodes.forEach { node ->
                val src = Path.of(node.path)

                val dstPath = when (destination) {
                    is ExplorerNode.File -> destination.parentPath

                    is ExplorerNode.Directory -> destination.path
                }

                val dst = Path.of(dstPath).resolve(src.name)

                transfer(src = src, dst = dst, overwrite = overwrite, copy = true)
            }
        }

        override suspend fun moveNodes(
            nodes: Set<ExplorerNode>, destination: ExplorerNode, overwrite: Boolean
        ) = runCatching {
            nodes.forEach { node ->
                val src = Path.of(node.path)

                val dstPath = when (destination) {
                    is ExplorerNode.File -> destination.parentPath

                    is ExplorerNode.Directory -> destination.path
                }

                val dst = Path.of(dstPath)

                transfer(src = src, dst = dst, overwrite = overwrite, copy = false)
            }
        }

        override suspend fun deleteNodes(nodes: Set<ExplorerNode>) = runCatching {
            nodes.forEach { node ->
                val path = Path.of(node.path)

                checkPathExists(path = path)

                when {
                    path.isRegularFile() -> if (!path.toFile().delete()) {
                        throw ExplorerException("Failed to delete '${path.absolutePathString()}' file")
                    }

                    path.isDirectory() -> if (!path.toFile().deleteRecursively()) {
                        throw ExplorerException("Failed to delete '${path.absolutePathString()}' directory")
                    }
                }
            }
        }

        override fun close() = coroutineScope.cancel()
    }
}