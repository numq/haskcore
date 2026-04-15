package io.github.numq.haskcore.service.vfs

import androidx.datastore.core.DataStore
import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import kotlin.streams.asSequence

internal class LocalVfsDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<SnapshotData?>
) : VfsDataSource {
    private fun createVirtualFile(path: String): VirtualFile? {
        val nioPath = Path.of(path)

        val name = nioPath.fileName.toString()

        val nameWithoutExtension = nioPath.nameWithoutExtension

        val extension = when {
            name.startsWith(".") && name.count('.'::equals) == 1 -> null

            else -> nioPath.extension.ifEmpty { null }
        }

        return when {
            nioPath.exists() -> VirtualFile(
                path = path,
                name = name,
                nameWithoutExtension = nameWithoutExtension,
                extension = extension,
                isDirectory = nioPath.isDirectory(),
                isMetadata = path.contains(".haskcore") || name.endsWith(".tmp"),
                size = when {
                    nioPath.isRegularFile() -> nioPath.fileSize()

                    else -> 0L
                },
                lastModifiedTimestamp = Timestamp(nioPath.getLastModifiedTime().to(TimeUnit.NANOSECONDS))
            )

            else -> null
        }
    }

    override val snapshotData = dataStore.data

    override suspend fun getSnapshotData() = Either.catch { dataStore.data.first() }

    override suspend fun updateSnapshotData(transform: (SnapshotData?) -> SnapshotData?) = Either.catch {
        dataStore.updateData(transform)
    }

    override suspend fun fetchSingleEntry(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            createVirtualFile(path = path)
        }
    }

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
            Path.of(path).listDirectoryEntries()
        }.mapNotNull { entry ->
            createVirtualFile(path = entry.absolutePathString())
        }
    }

    override suspend fun listRecursive(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Files.walk(Path.of(path)).use { paths ->
                paths.asSequence().mapNotNull { path ->
                    createVirtualFile(path = path.absolutePathString())
                }.toList()
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

    override fun close() {
        scope.cancel()
    }
}