package io.github.numq.haskcore.feature.explorer.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ExplorerPositionData(
    @ProtoNumber(1) val index: Int = 0,
    @ProtoNumber(2) val offset: Int = 0,
)