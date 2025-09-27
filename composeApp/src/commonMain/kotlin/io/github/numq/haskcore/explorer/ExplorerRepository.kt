package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.clipboard.ClipboardService
import io.github.numq.haskcore.filesystem.FileSystemChange
import io.github.numq.haskcore.filesystem.FileSystemService
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.*
import kotlin.io.path.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface ExplorerRepository {
    suspend fun getNodes(rootPath: String): Result<Flow<List<ExplorerNode>>>

    suspend fun createFile(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun createDirectory(destination: ExplorerNode, name: String): Result<Unit>

    suspend fun expandDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun collapseDirectory(directory: ExplorerNode.Directory): Result<Unit>

    suspend fun renameNode(node: ExplorerNode, name: String): Result<Unit>

    suspend fun cutNodes(nodes: Set<ExplorerNode>): Result<Unit>

    suspend fun copyNodes(nodes: Set<ExplorerNode>): Result<Unit>

    suspend fun pasteNodes(destination: ExplorerNode): Result<Unit>

    suspend fun moveNodes(nodes: Set<ExplorerNode>, destination: ExplorerNode): Result<Unit>

    suspend fun deleteNodes(nodes: Set<ExplorerNode>): Result<Unit>

    class Default(
        private val clipboardService: ClipboardService, private val fileSystemService: FileSystemService
    ) : ExplorerRepository {
        private val operations = MutableSharedFlow<ExplorerOperation>()

        private suspend fun buildNode(rootPath: String, path: String) = Path(path).runCatching {
            val parentPath = when (path) {
                rootPath -> path

                else -> parent?.absolutePathString() ?: return@runCatching null
            }

            val depth = when (path) {
                rootPath -> 0

                else -> Path(rootPath).relativize(Path(path)).nameCount
            }

            val lastModified = getLastModifiedTime().toMillis()

            when {
                fileSystemService.isFile(path = path).getOrThrow() -> ExplorerNode.File(
                    name = name,
                    path = path,
                    parentPath = parentPath,
                    depth = depth,
                    lastModified = lastModified,
                    extension = extension
                )

                fileSystemService.isDirectory(path = path).getOrThrow() -> ExplorerNode.Directory(
                    name = name,
                    path = path,
                    parentPath = parentPath,
                    depth = depth,
                    lastModified = lastModified,
                    expanded = false
                )

                else -> null
            }
        }.getOrNull()

        private suspend fun buildChildren(
            rootPath: String, path: String
        ) = fileSystemService.listDirectory(path = path).getOrThrow().mapNotNull { childPath ->
            buildNode(rootPath = rootPath, path = childPath)
        }.toPersistentSet()

        private fun buildFlattened(tree: ExplorerTree) = buildList {
            fun visit(node: ExplorerNode) {
                add(node)

                if (node is ExplorerNode.Directory && node.expanded) {
                    tree.childrenByPath[node.path].orEmpty().sortedWith(ExplorerNodeComparator).forEach(::visit)
                }
            }

            visit(node = tree.root)
        }

        private suspend fun handleExpand(tree: ExplorerTree, directory: ExplorerNode.Directory, rootPath: String) =
            when {
                directory.expanded -> tree

                else -> updateNodeInTree(
                    tree = tree,
                    updatedNode = directory.copy(expanded = true),
                    children = buildChildren(rootPath = rootPath, path = directory.path)
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
            tree: ExplorerTree, updatedNode: ExplorerNode.Directory, children: PersistentSet<ExplorerNode>? = null
        ) = when {
            updatedNode.path == tree.root.path -> {
                var newChildrenByPath = tree.childrenByPath

                children?.let { childNode ->
                    newChildrenByPath = newChildrenByPath.put(updatedNode.path, childNode)
                }

                tree.copy(root = updatedNode, childrenByPath = newChildrenByPath)
            }

            else -> {
                val parentPath = updatedNode.parentPath

                val currentChildren = tree.childrenByPath[parentPath] ?: return tree

                val newChildren = currentChildren.map { node ->
                    if (node.path == updatedNode.path) updatedNode else node
                }.toPersistentSet()

                var newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

                children?.let { childNode ->
                    newChildrenByPath = newChildrenByPath.put(updatedNode.path, childNode)
                }

                tree.copy(childrenByPath = newChildrenByPath)
            }
        }

        private suspend fun handleChange(
            tree: ExplorerTree,
            change: FileSystemChange,
            rootPath: String
        ) = when (change) {
            is FileSystemChange.Created -> handleCreated(tree = tree, change = change, rootPath = rootPath)

            is FileSystemChange.Modified -> handleModified(tree = tree, change = change, rootPath = rootPath)

            is FileSystemChange.Deleted -> handleDeleted(tree = tree, change = change)
        }

        private suspend fun handleCreated(
            tree: ExplorerTree, change: FileSystemChange.Created, rootPath: String
        ): ExplorerTree {
            val parentPath = change.parentPath ?: rootPath

            val newNode = buildNode(rootPath = rootPath, path = change.path) ?: return tree

            val currentChildren = tree.childrenByPath[parentPath] ?: emptyList()

            val newChildren = when {
                currentChildren.any { childNode -> childNode.path == change.path } -> currentChildren.map { childNode ->
                    when {
                        childNode.path == change.path -> newNode

                        else -> childNode
                    }
                }

                else -> currentChildren + newNode
            }.toPersistentSet()

            var newTree = tree.copy(childrenByPath = tree.childrenByPath.put(parentPath, newChildren))

            if (newNode is ExplorerNode.Directory) {
                val parentDirectory = findDirectoryInTree(tree = tree, path = parentPath)

                if (parentDirectory != null && parentDirectory.expanded) {
                    val expandedNewNode = newNode.copy(expanded = true)

                    val newChildrenForParent = newChildren.map { childNode ->
                        when {
                            childNode.path == change.path -> expandedNewNode

                            else -> childNode
                        }
                    }.toPersistentSet()

                    val childrenOfNewNode = buildChildren(rootPath = rootPath, path = expandedNewNode.path)

                    newTree = newTree.copy(
                        childrenByPath = newTree.childrenByPath.put(parentPath, newChildrenForParent)
                            .put(expandedNewNode.path, childrenOfNewNode)
                    )
                }
            }

            return newTree
        }

        private fun findDirectoryInTree(tree: ExplorerTree, path: String): ExplorerNode.Directory? {
            if (tree.root.path == path) {
                return tree.root
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

            return tree.childrenByPath[tree.root.path]?.let(::findInChildren)
        }

        private suspend fun handleModified(
            tree: ExplorerTree, change: FileSystemChange.Modified, rootPath: String
        ): ExplorerTree {
            val parentPath = change.parentPath ?: rootPath

            val modifiedNode = buildNode(rootPath = rootPath, path = change.path) ?: return tree

            val currentChildren = tree.childrenByPath[parentPath] ?: return tree

            val oldNode = currentChildren.find { childNode -> childNode.path == change.path }

            val newChildren = currentChildren.map { node ->
                when {
                    node.path == change.path -> when {
                        modifiedNode is ExplorerNode.Directory -> modifiedNode.copy(expanded = false)

                        else -> modifiedNode
                    }

                    else -> node
                }
            }.toPersistentSet()

            var newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

            if (modifiedNode is ExplorerNode.Directory) {
                newChildrenByPath = newChildrenByPath.remove(change.path)

                val wasExpanded = oldNode is ExplorerNode.Directory && oldNode.expanded

                if (wasExpanded) {
                    val pathsToRemove = tree.childrenByPath.keys.filter { path ->
                        path.startsWith(change.path) && path != change.path
                    }

                    pathsToRemove.forEach { path ->
                        newChildrenByPath = newChildrenByPath.remove(path)
                    }
                }
            }

            return tree.copy(childrenByPath = newChildrenByPath)
        }

        private fun handleDeleted(tree: ExplorerTree, change: FileSystemChange.Deleted): ExplorerTree {
            val parentPath = change.parentPath ?: return tree

            val currentChildren = tree.childrenByPath[parentPath] ?: return tree

            val newChildren = currentChildren.filter { childNode -> childNode.path != change.path }.toPersistentSet()

            val newChildrenByPath = tree.childrenByPath.put(parentPath, newChildren)

            return tree.copy(childrenByPath = newChildrenByPath)
        }

        override suspend fun getNodes(rootPath: String) = runCatching {
            require(rootPath.isNotBlank()) { "Root path cannot be blank" }

            val root = with(Path(rootPath)) {
                ExplorerNode.Directory(
                    name = name,
                    path = rootPath,
                    parentPath = rootPath,
                    depth = 0,
                    lastModified = getLastModifiedTime().toMillis(),
                    expanded = false
                )
            }

            val rootChildren = buildChildren(rootPath = rootPath, path = rootPath)

            val initialTree = ExplorerTree(root = root, childrenByPath = persistentMapOf(rootPath to rootChildren))

            val changes = fileSystemService.observeDirectoryChanges(path = rootPath).getOrThrow()

            merge(operations, changes.map(ExplorerOperation::Change)).scan(initialTree) { tree, operation ->
                when (operation) {
                    is ExplorerOperation.Expand -> handleExpand(
                        tree = tree,
                        directory = operation.directory,
                        rootPath = rootPath
                    )

                    is ExplorerOperation.Collapse -> handleCollapse(tree = tree, directory = operation.directory)

                    is ExplorerOperation.Change -> handleChange(
                        tree = tree,
                        change = operation.change,
                        rootPath = rootPath
                    )
                }
            }.map { tree ->
                buildFlattened(tree = tree)
            }.onStart {
                emit(buildFlattened(tree = initialTree))
            }.distinctUntilChanged()
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

        override suspend fun cutNodes(nodes: Set<ExplorerNode>) = runCatching {
            clipboardService.cut(paths = nodes.map(ExplorerNode::path)).getOrThrow()
        }

        override suspend fun copyNodes(nodes: Set<ExplorerNode>) = runCatching {
            clipboardService.copy(paths = nodes.map(ExplorerNode::path)).getOrThrow()
        }

        override suspend fun pasteNodes(destination: ExplorerNode) = runCatching {
            val destinationPath = when (destination) {
                is ExplorerNode.File -> destination.parentPath

                is ExplorerNode.Directory -> destination.path
            }

            clipboardService.paste(path = destinationPath).getOrThrow()
        }

        override suspend fun moveNodes(nodes: Set<ExplorerNode>, destination: ExplorerNode) = runCatching {
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

        override suspend fun deleteNodes(nodes: Set<ExplorerNode>) = runCatching {
            nodes.forEach { node ->
                fileSystemService.delete(path = node.path).getOrThrow()
            }
        }
    }
}