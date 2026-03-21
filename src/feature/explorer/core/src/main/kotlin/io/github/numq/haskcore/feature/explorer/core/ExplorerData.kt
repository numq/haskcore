package io.github.numq.haskcore.feature.explorer.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ExplorerData(
    @ProtoNumber(1) val expandedPaths: List<String> = emptyList(),
    @ProtoNumber(2) val index: Int = 0,
    @ProtoNumber(3) val offset: Int = 0
)