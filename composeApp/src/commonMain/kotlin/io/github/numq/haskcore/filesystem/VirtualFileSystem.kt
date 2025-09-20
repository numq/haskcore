package io.github.numq.haskcore.filesystem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

@OptIn(ExperimentalTime::class)
interface VirtualFileSystem {
    fun getNode(path: String): FileSystemNode

    fun getChildren(path: String): List<FileSystemNode>

    fun watch(path: String): Flow<FileSystemEvent>

    fun invalidateCache(path: String)

    fun invalidateCache()

    class Default : VirtualFileSystem {
        private val cacheLock = ReentrantLock()

        private val nodeCache = mutableMapOf<String, FileSystemNode>()

        private val childrenCache = mutableMapOf<String, List<FileSystemNode>>()

        private fun File.getSize() = when {
            isDirectory -> walk().filter(File::isFile).fold(0L) { size, file ->
                size + file.length()
            }

            else -> length()
        }

        private fun File.getPermissions() = buildString {
            append(if (canRead()) "r" else "-")
            append(if (canWrite()) "w" else "-")
            append(if (canExecute()) "x" else "-")
        }

        private fun File.getTime() = with(Files.readAttributes(toPath(), BasicFileAttributes::class.java)) {
            val created = runCatching {
                creationTime().toInstant().toKotlinInstant()
            }.getOrDefault(Clock.System.now())

            val accessed = runCatching {
                lastAccessTime().toInstant().toKotlinInstant()
            }.getOrDefault(Clock.System.now())

            val modified = runCatching {
                lastModifiedTime().toInstant().toKotlinInstant()
            }.getOrDefault(Clock.System.now())

            Triple(created, accessed, modified)
        }

        private fun buildNode(
            path: String, parent: FileSystemNode? = null
        ): FileSystemNode = with(File(path)) {
            if (!exists()) {
                throw IOException("Path $path does not exist")
            }

            val absolutePath = Path(path).toAbsolutePath().toString()

            nodeCache[absolutePath]?.let { cached ->
                val node = when (cached) {
                    is FileSystemNode.File -> cached.copy(parent = parent)

                    is FileSystemNode.Directory -> cached.copy(parent = parent)
                }

                nodeCache[absolutePath] = node

                return node
            }

            val size = getSize()

            val isReadOnly = !canWrite()

            val permissions = getPermissions()

            val (createdAt, accessedAt, modifiedAt) = getTime()

            when {
                isDirectory -> {
                    val children = listFiles()?.map { file ->
                        buildNode(path = file.absolutePath, parent = null)
                    } ?: emptyList()

                    val node = FileSystemNode.Directory(
                        name = name,
                        path = absolutePath,
                        size = size,
                        isHidden = isHidden,
                        isReadOnly = isReadOnly,
                        permissions = permissions,
                        createdAt = createdAt,
                        accessedAt = accessedAt,
                        modifiedAt = modifiedAt,
                        parent = parent,
                        children = children
                    )

                    children.forEach { child ->
                        nodeCache[child.path] = when (child) {
                            is FileSystemNode.File -> child.copy(parent = node)

                            is FileSystemNode.Directory -> child.copy(parent = node)
                        }
                    }

                    nodeCache[absolutePath] = node

                    childrenCache[absolutePath] = children

                    node
                }

                else -> {
                    val node = FileSystemNode.File(
                        name = name,
                        path = absolutePath,
                        size = size,
                        isHidden = isHidden,
                        isReadOnly = isReadOnly,
                        permissions = permissions,
                        createdAt = createdAt,
                        accessedAt = accessedAt,
                        modifiedAt = modifiedAt,
                        parent = parent
                    )

                    nodeCache[absolutePath] = node

                    node
                }
            }
        }

        override fun getNode(path: String) = cacheLock.withLock {
            val absolutePath = Path(path).toAbsolutePath().toString()

            nodeCache[absolutePath] ?: buildNode(absolutePath)
        }

        override fun getChildren(path: String) = cacheLock.withLock {
            val absolutePath = Path(path).toAbsolutePath().toString()

            childrenCache[absolutePath] ?: (buildNode(absolutePath) as? FileSystemNode.Directory)?.children
            ?: emptyList()
        }

        override fun watch(path: String) = channelFlow {
            val watchPath = Path(path)

            withContext(Dispatchers.IO) {
                val watchService = watchPath.fileSystem.newWatchService()

                try {
                    watchPath.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )

                    while (isActive) {
                        val key = runInterruptible { watchService.take() }

                        try {
                            for (event in key.pollEvents()) {
                                val context = event.context() as Path

                                val fullPath = (key.watchable() as Path).resolve(context).toAbsolutePath().toString()

                                val fileSystemEvent = when (event.kind()) {
                                    StandardWatchEventKinds.ENTRY_CREATE -> FileSystemEvent.Created(path = fullPath)

                                    StandardWatchEventKinds.ENTRY_MODIFY -> FileSystemEvent.Modified(path = fullPath)

                                    StandardWatchEventKinds.ENTRY_DELETE -> FileSystemEvent.Deleted(path = fullPath)

                                    else -> null
                                }

                                if (fileSystemEvent != null) {
                                    invalidateCache(path = fullPath)

                                    send(fileSystemEvent)
                                }
                            }
                        } finally {
                            key.reset()
                        }
                    }
                } finally {
                    watchService.close()
                }
            }
        }

        override fun invalidateCache(path: String) {
            val absolutePath = Path(path).toAbsolutePath().toString()

            val pathsToInvalidate = mutableSetOf<String>()

            fun collectPaths(currentPath: String) {
                if (currentPath in nodeCache) {
                    pathsToInvalidate.add(currentPath)

                    childrenCache[currentPath]?.forEach { child ->
                        collectPaths(child.path)
                    }
                }
            }

            cacheLock.withLock {
                collectPaths(currentPath = absolutePath)

                pathsToInvalidate.forEach { pathToInvalidate ->
                    nodeCache.remove(pathToInvalidate)

                    childrenCache.remove(pathToInvalidate)
                }
            }
        }

        override fun invalidateCache() {
            cacheLock.withLock {
                nodeCache.clear()

                childrenCache.clear()
            }
        }
    }
}