package io.github.numq.haskcore.service.vfs

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class SnapshotData(
    @ProtoNumber(1) val path: String,
    @ProtoNumber(2) val files: Map<String, List<VirtualFileData>>,
    @ProtoNumber(3) val timestampNanos: Long,
)