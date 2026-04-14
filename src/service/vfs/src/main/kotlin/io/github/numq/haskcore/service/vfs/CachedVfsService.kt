package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class CachedVfsService(
    private val scope: CoroutineScope, private val vfsDataSource: VfsDataSource
) : VfsService {
    private companion object {
        const val BUFFER_CAPACITY = 64

        const val AUTO_SAVE_DEBOUNCE_MILLIS = 2_000L
    }

    private val watchFlows = ConcurrentHashMap<String, Flow<VfsEvent>>()

    private val cacheUpdates = MutableSharedFlow<VfsCacheAction>(extraBufferCapacity = BUFFER_CAPACITY)

    private val directoryCache = cacheUpdates.scan(
        initial = emptyMap<String, Map<String, VirtualFile>>()
    ) { accumulator, action ->
        when (action) {
            is VfsCacheAction.SetDirectory -> accumulator + (action.path to action.files)

            is VfsCacheAction.RemoveDirectory -> accumulator - action.path

            is VfsCacheAction.UpdateEntry -> {
                val currentDir = accumulator[action.parentPath] ?: return@scan accumulator

                val fileName = Path.of(action.event.path).fileName.toString()

                val nextDir = when (action.event) {
                    is VfsEvent.Created, is VfsEvent.Modified -> when (action.newFile) {
                        null -> currentDir

                        else -> currentDir + (fileName to action.newFile)
                    }

                    is VfsEvent.Deleted -> currentDir - fileName
                }

                when (currentDir) {
                    nextDir -> accumulator

                    else -> accumulator + (action.parentPath to nextDir)
                }
            }
        }
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyMap())

    init {
        scope.launch {
            vfsDataSource.getSnapshotData().onRight { snapshotData ->
                snapshotData?.toSnapshot()?.let { snapshot ->
                    snapshot.files.forEach { (parentPath, files) ->


                        cacheUpdates.emit(
                            VfsCacheAction.SetDirectory(
                                path = parentPath, files = files.associateBy(VirtualFile::name)
                            )
                        )
                    }
                    backgroundSync(rootPath = snapshot.path)
                }
            }
        }

        directoryCache.debounce(AUTO_SAVE_DEBOUNCE_MILLIS).onEach { cache ->
            if (cache.isNotEmpty()) {
                val rootPath = cache.keys.minByOrNull(String::length) ?: return@onEach

                val snapshotFiles = cache.mapValues { files -> files.value.values.toList() }

                val snapshot = Snapshot(
                    path = rootPath, timestamp = Timestamp.now(), files = snapshotFiles
                )

                vfsDataSource.updateSnapshotData { snapshot.toSnapshotData() }
            }
        }.launchIn(scope)
    }

    private suspend fun backgroundSync(rootPath: String) = vfsDataSource.listRecursive(rootPath).onRight { allFiles ->
        allFiles.groupBy { file ->
            Path.of(file.path).parent?.toString() ?: rootPath
        }.forEach { (parentPath, files) ->
            cacheUpdates.emit(
                VfsCacheAction.SetDirectory(
                    path = parentPath, files = files.associateBy(VirtualFile::name)
                )
            )
        }
    }

    override suspend fun observeFiles(path: String): Either<Throwable, Flow<List<VirtualFile>>> = either {
        if (!directoryCache.value.containsKey(path)) {
            val files = vfsDataSource.list(path = path).bind()

            cacheUpdates.emit(VfsCacheAction.SetDirectory(path = path, files = files.associateBy(VirtualFile::name)))
        }

        val watchEvents = watchFlows.getOrPut(path) {
            vfsDataSource.watch(path).bind().shareIn(scope = scope, started = SharingStarted.Eagerly)
        }

        channelFlow {
            val job = watchEvents.onEach { event ->
                val newFile = when (event) {
                    is VfsEvent.Deleted -> null

                    else -> vfsDataSource.fetchSingleEntry(path = event.path).getOrNull()
                }

                cacheUpdates.emit(VfsCacheAction.UpdateEntry(parentPath = path, event = event, newFile = newFile))
            }.launchIn(this)

            directoryCache.mapNotNull { cache -> cache[path]?.values?.toList() }.distinctUntilChanged().collect(::send)

            awaitClose {
                job.cancel()
            }
        }
    }

    override suspend fun observeVisibleFiles(path: String) = observeFiles(path = path).map { flow ->
        flow.map { files -> files.filterNot(VirtualFile::isMetadata) }
    }

    override suspend fun create(path: String, isDirectory: Boolean) = vfsDataSource.create(
        path = path, isDirectory = isDirectory
    )

    override suspend fun move(src: String, dst: String, overwrite: Boolean) = vfsDataSource.move(
        src = src, dst = dst, overwrite = overwrite
    )

    override suspend fun copy(src: String, dst: String, overwrite: Boolean) = vfsDataSource.copy(
        src = src, dst = dst, overwrite = overwrite
    )

    override suspend fun delete(path: String) = vfsDataSource.delete(path = path).flatMap {
        Either.catch {
            watchFlows.remove(path)

            cacheUpdates.emit(VfsCacheAction.RemoveDirectory(path = path))

            val parentPath = Path.of(path).parent?.toString()

            if (parentPath != null) {
                cacheUpdates.emit(
                    VfsCacheAction.UpdateEntry(
                        parentPath = parentPath, event = VfsEvent.Deleted(path = path), newFile = null
                    )
                )
            }
        }
    }

    override fun close() {
        scope.cancel()

        watchFlows.clear()
    }
}