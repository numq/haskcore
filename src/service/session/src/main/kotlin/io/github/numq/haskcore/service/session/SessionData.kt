package io.github.numq.haskcore.service.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class SessionData(
    @ProtoNumber(1) val active: List<SessionRecordData> = emptyList(),
    @ProtoNumber(2) val history: Map<String, SessionRecordData> = emptyMap()
)