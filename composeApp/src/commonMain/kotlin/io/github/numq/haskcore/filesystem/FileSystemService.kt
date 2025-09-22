package io.github.numq.haskcore.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.io.File
import java.io.IOException
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface FileSystemService {
    fun exists(path: String): Result<Boolean>

    fun isFile(path: String): Result<Boolean>

    fun isDirectory(path: String): Result<Boolean>

    fun readBytes(path: String): Result<ByteArray>

    fun readText(path: String): Result<String>

    fun readLines(path: String): Result<List<String>>

    fun getNodes(path: String, recursive: Boolean): Result<List<FileSystemNode>>

    fun listDirectory(path: String, recursive: Boolean): Result<List<FileSystemNode>>

    fun observeDirectory(path: String): Result<Flow<FileSystemNodeChange>>

    fun createFile(path: String, bytes: ByteArray): Result<Unit>

    fun createFile(path: String, text: String): Result<Unit>

    fun createDirectory(path: String): Result<Unit>

    fun rename(path: String, name: String): Result<Unit>

    fun move(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    fun copy(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    fun delete(path: String): Result<Unit>

    class Default(private val virtualFileSystem: VirtualFileSystem) : FileSystemService {
        private fun createFileInternal(path: String, block: File.() -> Unit) = runCatching {
            try {
                with(File(path)) {
                    parentFile?.mkdirs()

                    block(this)
                }
            } finally {
                virtualFileSystem.invalidateCache(path)

                invalidateParent(path)
            }
        }

        private fun getAllDescendants(
            dir: FileSystemNode.Directory
        ): List<FileSystemNode> = dir.children.flatMap { child ->
            when (child) {
                is FileSystemNode.Directory -> listOf(child) + getAllDescendants(child)

                else -> listOf(child)
            }
        }

        private fun invalidateParent(path: String) =
            File(path).parentFile?.toPath()?.toAbsolutePath()?.toString()?.let(virtualFileSystem::invalidateCache)

        override fun exists(path: String) = runCatching {
            try {
                virtualFileSystem.getNode(path)

                true
            } catch (_: IOException) {
                false
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to check if '$path' exists: ${throwable.message}")
        }

        override fun isFile(path: String) = runCatching {
            virtualFileSystem.getNode(path) is FileSystemNode.File
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to check if '$path' is file: ${throwable.message}")
        }

        override fun isDirectory(path: String) = runCatching {
            virtualFileSystem.getNode(path) is FileSystemNode.Directory
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to check if '$path' is directory: ${throwable.message}")
        }

        override fun readBytes(path: String) = runCatching {
            val file = File(path)

            if (!file.exists()) {
                throw IOException("Path '$path' does not exist")
            }

            file.readBytes()
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to read bytes for '$path': ${throwable.message}")
        }

        override fun readText(path: String) = runCatching {
            val file = File(path)

            if (!file.exists()) {
                throw IOException("Path '$path' does not exist")
            }

            file.readText()
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to read text for '$path': ${throwable.message}")
        }

        override fun readLines(path: String) = runCatching {
            val file = File(path)

            if (!file.exists()) {
                throw IOException("Path '$path' does not exist")
            }

            file.readLines()
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to read lines for '$path': ${throwable.message}")
        }

        override fun getNodes(path: String, recursive: Boolean) = runCatching {
            when (val node = virtualFileSystem.getNode(path)) {
                is FileSystemNode.Directory -> listOf(node) + when {
                    recursive -> getAllDescendants(node)

                    else -> node.children
                }

                else -> error("'$path' is not a directory")
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to get nodes for '$path': ${throwable.message}")
        }

        override fun listDirectory(path: String, recursive: Boolean) = runCatching {
            when (val node = virtualFileSystem.getNode(path)) {
                is FileSystemNode.Directory -> when {
                    recursive -> getAllDescendants(node)

                    else -> node.children
                }

                else -> error("'$path' is not a directory")
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to list directory '$path': ${throwable.message}")
        }

        override fun observeDirectory(path: String) = runCatching {
            val rootNode =
                virtualFileSystem.getNode(path) as? FileSystemNode.Directory ?: error("'$path' is not a directory")

            val nodes = mutableMapOf<String, FileSystemNode>()

            nodes[path] = rootNode

            virtualFileSystem.watch(path).mapNotNull { event ->
                when (event) {
                    is FileSystemEvent.Created -> {
                        val createdNode = virtualFileSystem.getNode(event.path)

                        nodes[createdNode.path] = createdNode

                        createdNode.parent?.let { parent ->
                            val parentNode = nodes[parent.path] as? FileSystemNode.Directory

                            if (parentNode != null) {
                                nodes[parent.path] = parentNode.copy(children = parentNode.children + createdNode)
                            }
                        }

                        FileSystemNodeChange.Added(createdNode)
                    }

                    is FileSystemEvent.Modified -> {
                        val modifiedNode = virtualFileSystem.getNode(event.path)

                        nodes[modifiedNode.path] = modifiedNode

                        FileSystemNodeChange.Updated(modifiedNode)
                    }

                    is FileSystemEvent.Deleted -> {
                        val removedPath = event.path

                        val deletedNode = nodes.remove(removedPath)

                        deletedNode?.parent?.let { parent ->
                            val parentNode = nodes[parent.path] as? FileSystemNode.Directory

                            if (parentNode != null) {
                                nodes[parent.path] = parentNode.copy(children = parentNode.children.filter { node ->
                                    node.path != removedPath
                                })
                            }
                        }

                        FileSystemNodeChange.Removed(removedPath)
                    }
                }
            }
        }

        override fun createFile(path: String, bytes: ByteArray) = createFileInternal(path) {
            writeBytes(bytes)
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create file '$path': ${throwable.message}")
        }

        override fun createFile(path: String, text: String) = createFileInternal(path) {
            writeText(text)
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create file '$path': ${throwable.message}")
        }

        override fun createDirectory(path: String) = runCatching {
            try {
                if (!File(path).mkdirs()) {
                    throw IOException("File system error")
                }
            } finally {
                virtualFileSystem.invalidateCache(path)

                invalidateParent(path)
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to create directory '$path': ${throwable.message}")
        }

        override fun rename(path: String, name: String) = runCatching {
            val srcFile = File(path)

            val dstFile = File(srcFile.parent, name)

            try {
                if (!srcFile.exists()) {
                    throw IOException("Path '$path' does not exist")
                }

                if (dstFile.exists()) {
                    throw IOException("Target '$name' already exists")
                }

                if (!srcFile.renameTo(dstFile)) {
                    throw IOException("File system error")
                }
            } finally {
                virtualFileSystem.invalidateCache(path)

                virtualFileSystem.invalidateCache(dstFile.toPath().toAbsolutePath().toString())

                invalidateParent(path)
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to rename '$path' to '$name': ${throwable.message}")
        }

        override fun move(fromPath: String, toPath: String, overwrite: Boolean) = runCatching {
            val srcFile = File(fromPath)

            val dstFile = File(toPath)

            try {
                if (!srcFile.exists()) {
                    throw IOException("Path '$fromPath' does not exist")
                }

                if (dstFile.exists() && !overwrite) {
                    throw IOException("Target '$toPath' already exists")
                }

                when {
                    srcFile.isDirectory -> {
                        srcFile.copyRecursively(dstFile, overwrite)

                        if (!srcFile.deleteRecursively()) {
                            dstFile.deleteRecursively()

                            throw IOException("File system error")
                        }
                    }

                    else -> {
                        srcFile.copyTo(dstFile, overwrite)

                        if (!srcFile.delete()) {
                            dstFile.delete()

                            throw IOException("File system error")
                        }
                    }
                }
            } finally {
                virtualFileSystem.invalidateCache(fromPath)

                virtualFileSystem.invalidateCache(toPath)

                invalidateParent(fromPath)

                invalidateParent(toPath)
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to move $fromPath to $toPath: ${throwable.message}")
        }

        override fun copy(fromPath: String, toPath: String, overwrite: Boolean) = runCatching {
            val srcFile = File(fromPath)

            val dstFile = File(toPath)

            try {
                if (!srcFile.exists()) {
                    throw IOException("Path '$fromPath' does not exist")
                }

                if (dstFile.exists() && !overwrite) {
                    throw IOException("Target '$toPath' already exists")
                }

                when {
                    srcFile.isDirectory -> srcFile.copyRecursively(dstFile, overwrite)

                    else -> srcFile.copyTo(dstFile, overwrite)
                }
            } finally {
                virtualFileSystem.invalidateCache(toPath)

                invalidateParent(toPath)
            }

            Unit
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to copy $fromPath to $toPath: ${throwable.message}")
        }

        override fun delete(path: String) = runCatching {
            val file = File(path)

            try {
                if (!file.exists()) {
                    throw FileSystemException("Path '$path' does not exist")
                }

                when {
                    file.isDirectory -> if (!file.deleteRecursively()) {
                        throw IOException("File system error")
                    }

                    !file.delete() -> throw IOException("File system error")
                }
            } finally {
                virtualFileSystem.invalidateCache(path)

                invalidateParent(path)
            }
        }.recoverCatching { throwable ->
            throw FileSystemException("Failed to delete '$path': ${throwable.message}")
        }
    }
}