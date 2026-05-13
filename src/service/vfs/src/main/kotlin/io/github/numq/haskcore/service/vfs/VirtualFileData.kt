package io.github.numq.haskcore.service.vfs

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class VirtualFileData(
    @ProtoNumber(1) val path: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val nameWithoutExtension: String,
    @ProtoNumber(4) val extension: String?,
    @ProtoNumber(5) val isDirectory: Boolean,
    @ProtoNumber(6) val isMetadata: Boolean,
    @ProtoNumber(7) val size: Long,
    @ProtoNumber(8) val lastModifiedTimestampNanos: Long,
)