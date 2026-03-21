package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

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
    ): List<VirtualFile> {
        val next = when (event) {
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

        return when (next) {
            files -> files

            else -> next
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun observeFiles(path: String) = either {
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

                    else -> vfsDataSource.fetchSingleEntry(path = event.path).getOrNull()
                }

                cacheUpdates.emit(VfsCacheAction.UpdateEntry(parentPath = path, event = event, newFile = newFile))
            }.launchIn(scope = this)

            directoryCache.mapNotNull { cache -> cache[path] }.distinctUntilChanged().collect(::send)

            awaitClose {
                watchFlows.remove(path)
            }
        }
    }

    override suspend fun observeVisibleFiles(path: String) = observeFiles(path = path).map { flow ->
        flow.map { files ->
            files.filterNot(VirtualFile::isMetadata)
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