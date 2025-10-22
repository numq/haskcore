package io.github.numq.haskcore.document

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Document {
    val id: String

    val path: String

    val name: String

    val content: String

    val openedAt: Timestamp

    @Serializable
    data class ReadOnly(
        override val id: String,
        override val path: String,
        override val name: String,
        override val content: String,
        override val openedAt: Timestamp = Timestamp.now(),
    ) : Document

    @Serializable
    data class Writable(
        override val id: String,
        override val path: String,
        override val name: String,
        override val content: String,
        override val openedAt: Timestamp = Timestamp.now(),
        val position: Position = Position(),
        val isModified: Boolean = false,
    ) : Document {
        @Serializable
        data class Position(val line: Int = 0, val column: Int = 0, val scrollPosition: Double = .0)
    }
}