package io.github.numq.haskcore.feature.navigation.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class WorkspaceData(
    @ProtoNumber(1) val x: Float? = null,
    @ProtoNumber(2) val y: Float? = null,
    @ProtoNumber(3) val width: Float? = null,
    @ProtoNumber(4) val height: Float? = null,
    @ProtoNumber(5) val isFullscreen: Boolean? = null,
    @ProtoNumber(6) val ratio: Float? = null
)