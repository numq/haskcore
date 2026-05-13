package io.github.numq.haskcore.service.vfs

import io.github.numq.haskcore.common.core.timestamp.Timestamp

internal fun VirtualFile.toVirtualFileData() = VirtualFileData(
    path = path,
    name = name,
    nameWithoutExtension = nameWithoutExtension,
    extension = extension,
    isDirectory = isDirectory,
    isMetadata = isMetadata,
    size = size,
    lastModifiedTimestampNanos = lastModifiedTimestamp.nanoseconds
)

internal fun VirtualFileData.toVirtualFile() = VirtualFile(
    path = path,
    name = name,
    nameWithoutExtension = nameWithoutExtension,
    extension = extension,
    isDirectory = isDirectory,
    isMetadata = isMetadata,
    size = size,
    lastModifiedTimestamp = Timestamp(nanoseconds = lastModifiedTimestampNanos)
)

internal fun Snapshot.toSnapshotData() = SnapshotData(
    path = path, files = files.mapValues { (_, files) ->
        files.map(VirtualFile::toVirtualFileData)
    }, timestampNanos = timestamp.nanoseconds
)

internal fun SnapshotData.toSnapshot() = Snapshot(
    path = path, files = files.mapValues { (_, files) ->
        files.map(VirtualFileData::toVirtualFile)
    }, timestamp = Timestamp(nanoseconds = timestampNanos)
)