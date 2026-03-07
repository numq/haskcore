package io.github.numq.haskcore.feature.shelf.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ShelfPanelData(
    @ProtoNumber(1) val tools: List<ShelfToolData>,
    @ProtoNumber(2) val activeTool: ShelfToolData? = null,
    @ProtoNumber(3) val ratio: Float? = null
)