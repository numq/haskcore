package io.github.numq.haskcore.feature.output.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class OutputData(
    @ProtoNumber(1) val sessions: List<OutputSessionData> = emptyList(),
    @ProtoNumber(2) val activeSession: OutputSessionData? = null,
)