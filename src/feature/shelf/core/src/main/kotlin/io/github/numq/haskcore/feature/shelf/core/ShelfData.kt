package io.github.numq.haskcore.feature.shelf.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ShelfData(
    @ProtoNumber(1) val leftPanel: ShelfPanelData = ShelfPanelData(tools = listOf(ShelfToolData.Explorer)),
    @ProtoNumber(2) val rightPanel: ShelfPanelData = ShelfPanelData(tools = listOf(ShelfToolData.Log))
)