package io.github.numq.haskcore.feature.workspace.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class WorkspaceData(
    @ProtoNumber(1) val x: Float? = null,
    @ProtoNumber(2) val y: Float? = null,
    @ProtoNumber(3) val width: Float? = null,
    @ProtoNumber(4) val height: Float? = null,
    @ProtoNumber(5) val isFullscreen: Boolean = false,
    @ProtoNumber(6) val shelfData: ShelfData? = null,
    @ProtoNumber(7) val verticalRatio: Float? = null,
)