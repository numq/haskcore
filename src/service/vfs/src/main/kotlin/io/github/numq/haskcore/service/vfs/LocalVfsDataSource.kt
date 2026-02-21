package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

internal class LocalVfsDataSource : VfsDataSource {
    override suspend fun watch(path: String) = Either.catch {
        callbackFlow {
            val watcher = DirectoryWatcher.builder().path(Path.of(path)).listener { event ->
                val eventPath = event.path().absolutePathString()

                val event = when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE -> VfsEvent.Created(path = eventPath)

                    DirectoryChangeEvent.EventType.MODIFY -> VfsEvent.Modified(path = eventPath)

                    DirectoryChangeEvent.EventType.DELETE -> VfsEvent.Deleted(path = eventPath)

                    else -> null
                }

                event?.let(::trySend)
            }.build()

            val future = watcher.watchAsync()

            awaitClose {
                future.cancel(true)

                watcher.close()
            }
        }.buffer(Channel.UNLIMITED).flowOn(Dispatchers.IO)
    }

    override suspend fun list(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).listDirectoryEntries().map { entry ->
                VirtualFile(
                    path = entry.absolutePathString(),
                    name = entry.fileName.toString(),
                    extension = entry.extension,
                    isDirectory = entry.isDirectory(),
                    size = when {
                        entry.isRegularFile() -> entry.fileSize()

                        else -> 0L
                    },
                    lastModified = Timestamp(nanoseconds = entry.getLastModifiedTime().to(TimeUnit.NANOSECONDS))
                )
            }
        }
    }

    override suspend fun create(path: String, isDirectory: Boolean) = Either.catch {
        withContext(Dispatchers.IO) {
            val path = Path.of(path)

            when {
                isDirectory -> path.createDirectories()

                else -> {
                    path.parent?.createDirectories()

                    path.createFile()
                }
            }

            Unit
        }
    }

    override suspend fun move(src: String, dst: String, overwrite: Boolean) = Either.catch {
        withContext(Dispatchers.IO) {
            val src = Path.of(src)

            val dst = Path.of(dst)

            val options = when {
                overwrite -> arrayOf(StandardCopyOption.REPLACE_EXISTING)

                else -> emptyArray<CopyOption>()
            }

            val target = when {
                dst.isDirectory() -> dst.resolve(src.fileName)

                else -> dst
            }

            Files.move(src, target, *options)

            Unit
        }
    }

    override suspend fun copy(src: String, dst: String, overwrite: Boolean) = Either.catch {
        withContext(Dispatchers.IO) {
            val src = Path.of(src)

            val dst = Path.of(dst)

            val target = when {
                dst.isDirectory() -> dst.resolve(src.fileName)

                else -> dst
            }

            when {
                src.isRegularFile() -> {
                    val options = when {
                        overwrite -> arrayOf(StandardCopyOption.REPLACE_EXISTING)

                        else -> emptyArray<CopyOption>()
                    }

                    Files.copy(src, target, *options)
                }

                else -> src.toFile().walkTopDown().forEach { file ->
                    val relative = file.toPath().relativeTo(src)

                    val destination = target.resolve(relative)

                    when {
                        file.isDirectory -> destination.createDirectories()

                        else -> Files.copy(
                            file.toPath(), destination, when {
                                overwrite -> StandardCopyOption.REPLACE_EXISTING

                                else -> StandardCopyOption.COPY_ATTRIBUTES
                            }
                        )
                    }
                }
            }

            Unit
        }
    }

    override suspend fun delete(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val path = Path.of(path)

            when {
                path.isDirectory() -> {
                    val deleted = path.toFile().deleteRecursively()

                    if (!deleted && path.exists()) {
                        throw IOException("Failed to delete $path")
                    }
                }

                else -> path.deleteIfExists()
            }

            Unit
        }
    }
}