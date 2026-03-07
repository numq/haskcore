package io.github.numq.haskcore.feature.shelf.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ShelfToolData {
    @Serializable
    @SerialName("explorer")
    data object Explorer : ShelfToolData

    @Serializable
    @SerialName("log")
    data object Log : ShelfToolData
}