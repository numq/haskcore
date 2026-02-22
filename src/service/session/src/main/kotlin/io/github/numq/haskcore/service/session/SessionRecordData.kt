package io.github.numq.haskcore.service.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class SessionRecordData(
    @ProtoNumber(1) val path: String, @ProtoNumber(2) val name: String?, @ProtoNumber(3) val timestampNanos: Long
)