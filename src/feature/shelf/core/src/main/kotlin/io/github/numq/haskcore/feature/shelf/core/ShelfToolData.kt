package io.github.numq.haskcore.feature.shelf.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ShelfToolData {
    @Serializable
    @SerialName("Explorer")
    data object Explorer : ShelfToolData

    @Serializable
    @SerialName("Log")
    data object Log : ShelfToolData
}