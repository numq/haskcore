package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

internal class CachedVfsService(
    private val scope: CoroutineScope, private val vfsDataSource: VfsDataSource
) : VfsService {
    private val watchFlows = ConcurrentHashMap<String, Flow<VfsEvent>>()

    private val cacheUpdates = MutableSharedFlow<VfsCacheAction>()

    private val directoryCache = cacheUpdates.scan(
        emptyMap<String, List<VirtualFile>>()
    ) { accumulator, action ->
        when (action) {
            is VfsCacheAction.SetDirectory -> accumulator + (action.path to action.files)

            is VfsCacheAction.RemoveDirectory -> accumulator - action.path

            is VfsCacheAction.UpdateEntry -> {
                val currentList = accumulator[action.parentPath] ?: return@scan accumulator

                accumulator + (action.parentPath to applyEventToList(currentList, action.event, action.newFile))
            }
        }
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyMap())

    private fun applyEventToList(
        files: List<VirtualFile>, event: VfsEvent, newFile: VirtualFile?
    ) = when (event) {
        is VfsEvent.Created -> when (newFile) {
            null -> files

            else -> files.filterNot { file -> file.path == newFile.path } + newFile
        }

        is VfsEvent.Deleted -> files.filter { file -> file.path != event.path }

        is VfsEvent.Modified -> when (newFile) {
            null -> files

            else -> files.map { file ->
                when (file.path) {
                    event.path -> newFile

                    else -> file
                }
            }
        }
    }

    private fun fetchSingleEntry(path: String) = Either.catch {
        val nioPath = Path.of(path)

        when {
            nioPath.exists() -> VirtualFile(
                path = path,
                name = nioPath.fileName.toString(),
                extension = nioPath.extension,
                isDirectory = nioPath.isDirectory(),
                size = when {
                    nioPath.isRegularFile() -> nioPath.fileSize()

                    else -> 0L
                },
                lastModified = Timestamp(nioPath.getLastModifiedTime().to(TimeUnit.NANOSECONDS))
            )

            else -> null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun observeDirectory(path: String) = either {
        if (!directoryCache.value.containsKey(path)) {
            val files = vfsDataSource.list(path = path).bind()

            cacheUpdates.emit(VfsCacheAction.SetDirectory(path = path, files = files))
        }

        val watchEvents = watchFlows.getOrPut(path) {
            vfsDataSource.watch(path).bind().shareIn(scope = scope, started = SharingStarted.WhileSubscribed())
        }

        channelFlow {
            watchEvents.onEach { event ->
                val newFile = when (event) {
                    is VfsEvent.Deleted -> null

                    else -> fetchSingleEntry(path = event.path).getOrNull()
                }

                cacheUpdates.emit(VfsCacheAction.UpdateEntry(parentPath = path, event = event, newFile = newFile))
            }.launchIn(this)

            directoryCache.map { cache -> cache[path] }.filterNotNull().distinctUntilChanged().collect(::send)

            invokeOnClose {
                watchFlows.remove(path)
            }
        }
    }

    override suspend fun create(path: String, isDirectory: Boolean) = vfsDataSource.create(path, isDirectory)

    override suspend fun move(src: String, dst: String, overwrite: Boolean) = vfsDataSource.move(src, dst, overwrite)

    override suspend fun copy(src: String, dst: String, overwrite: Boolean) = vfsDataSource.copy(src, dst, overwrite)

    override suspend fun delete(path: String) = vfsDataSource.delete(path).flatMap {
        Either.catch {
            watchFlows.remove(path)

            Unit
        }
    }

    override fun close() {
        scope.cancel()
    }
}