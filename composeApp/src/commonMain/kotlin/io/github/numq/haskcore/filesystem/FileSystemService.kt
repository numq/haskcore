package io.github.numq.haskcore.filesystem

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface FileSystemService {
    suspend fun exists(path: String): Result<Boolean>

    suspend fun isFile(path: String): Result<Boolean>

    suspend fun isDirectory(path: String): Result<Boolean>

    suspend fun readBytes(path: String): Result<ByteArray>

    suspend fun readText(path: String): Result<String>

    suspend fun readLines(path: String): Result<List<String>>

    suspend fun listDirectory(path: String): Result<List<String>>

    suspend fun observeDirectoryChanges(path: String): Result<Flow<FileSystemChange>>

    suspend fun createFile(path: String, bytes: ByteArray): Result<Unit>

    suspend fun createFile(path: String, text: String): Result<Unit>

    suspend fun createDirectory(path: String): Result<Unit>

    suspend fun rename(path: String, name: String): Result<Unit>

    suspend fun move(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    suspend fun copy(fromPath: String, toPath: String, overwrite: Boolean): Result<Unit>

    suspend fun delete(path: String): Result<Unit>

    class Default : FileSystemService {
        private suspend fun <T> withFileSystem(block: () -> T) = withContext(Dispatchers.IO) {
            runCatching(block).recoverCatching { throwable ->
                throw FileSystemException(throwable)
            }
        }

        private fun checkPath(path: String): File {
            val file = File(path)

            check(file.exists()) { "Path '$path' does not exist" }

            return file
        }

        private fun checkPaths(fromPath: String, toPath: String, overwrite: Boolean): Pair<File, File> {
            val srcFile = File(fromPath)

            check(srcFile.exists()) { error("Path '$fromPath' does not exist") }

            val dstFile = File(toPath)

            check(!dstFile.exists() || overwrite) { "Path '$toPath' already exists" }

            return srcFile to dstFile
        }

        private fun File.transferTo(target: File, overwrite: Boolean, copy: Boolean) {
            val source = toPath()

            val options = when {
                overwrite -> arrayOf(StandardCopyOption.REPLACE_EXISTING)

                else -> emptyArray()
            }

            when {
                copy -> Files.copy(source, target.toPath(), *options)

                else -> Files.move(source, target.toPath(), *options)
            }
        }

        override suspend fun exists(path: String) = withFileSystem {
            File(path).exists()
        }

        override suspend fun isFile(path: String) = withFileSystem {
            File(path).isFile
        }

        override suspend fun isDirectory(path: String) = withFileSystem {
            File(path).isDirectory
        }

        override suspend fun readBytes(path: String) = withFileSystem {
            checkPath(path = path).readBytes()
        }

        override suspend fun readText(path: String) = withFileSystem {
            checkPath(path = path).readText()
        }

        override suspend fun readLines(path: String) = withFileSystem {
            checkPath(path = path).readLines()
        }

        override suspend fun listDirectory(path: String) = withFileSystem {
            with(checkPath(path = path)) {
                check(isDirectory) { "Path $path is not a directory" }

                listFiles().map { file ->
                    file.toPath().absolutePathString()
                }
            }
        }

        override suspend fun observeDirectoryChanges(path: String) = withFileSystem {
            with(checkPath(path = path)) {
                check(isDirectory) { "Directory $path does not exist or is not a directory" }

                callbackFlow {
                    val watcher = DirectoryWatcher.builder().path(toPath()).listener { event ->
                        val path = event.path().absolutePathString()

                        val parentPath = event.path().parent.absolutePathString()

                        when (event.eventType()) {
                            DirectoryChangeEvent.EventType.CREATE -> trySend(
                                FileSystemChange.Created(
                                    path = path,
                                    parentPath = parentPath
                                )
                            )

                            DirectoryChangeEvent.EventType.MODIFY -> trySend(
                                FileSystemChange.Modified(
                                    path = path,
                                    parentPath = parentPath
                                )
                            )

                            DirectoryChangeEvent.EventType.DELETE -> trySend(
                                FileSystemChange.Deleted(
                                    path = path,
                                    parentPath = parentPath
                                )
                            )

                            else -> Unit
                        }
                    }.build()

                    val future = watcher.watchAsync()

                    awaitClose {
                        future.cancel(true)

                        watcher.close()
                    }
                }.flowOn(Dispatchers.IO)
            }
        }

        override suspend fun createFile(path: String, bytes: ByteArray) = withFileSystem {
            with(File(path)) {
                check(!exists()) { "File '$path' already exists" }

                parentFile?.mkdirs()

                check(createNewFile()) { "Failed to create file '$path'" }
            }
        }

        override suspend fun createFile(path: String, text: String) = withFileSystem {
            with(File(path)) {
                check(!exists()) { "File '$path' already exists" }

                parentFile?.mkdirs()

                check(createNewFile()) { "Failed to create file '$path'" }
            }
        }

        override suspend fun createDirectory(path: String) = withFileSystem {
            with(File(path)) {
                if (!exists()) {
                    check(mkdirs()) { "Failed to create directory '$path'" }
                }
            }
        }

        override suspend fun rename(path: String, name: String) = withFileSystem {
            val srcFile = checkPath(path)

            val dstFile = File(srcFile.parent, name)

            check(!dstFile.exists()) { "File '$name' already exists in ${srcFile.parent}" }

            srcFile.transferTo(target = dstFile, overwrite = false, copy = false)
        }

        override suspend fun move(fromPath: String, toPath: String, overwrite: Boolean) = withFileSystem {
            val (srcFile, dstFile) = checkPaths(fromPath = fromPath, toPath = toPath, overwrite = overwrite)

            srcFile.transferTo(target = dstFile, overwrite = overwrite, copy = false)
        }

        override suspend fun copy(fromPath: String, toPath: String, overwrite: Boolean) = withFileSystem {
            val (srcFile, dstFile) = checkPaths(fromPath = fromPath, toPath = toPath, overwrite = overwrite)

            srcFile.transferTo(target = dstFile, overwrite = overwrite, copy = true)
        }

        override suspend fun delete(path: String) = withFileSystem {
            with(checkPath(path = path)) {
                when {
                    isDirectory -> check(deleteRecursively()) { "Failed to delete '$path' directory" }

                    isFile -> check(delete()) { "Failed to delete '$path' file" }
                }
            }
        }
    }
}