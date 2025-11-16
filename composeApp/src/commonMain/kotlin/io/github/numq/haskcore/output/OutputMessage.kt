package io.github.numq.haskcore.output

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal sealed interface OutputMessage {
    val id: String

    val text: String

    val timestamp: Timestamp

    @Serializable
    data class Info(override val text: String, override val timestamp: Timestamp = Timestamp.now()) : OutputMessage {
        override val id = "${UUID.randomUUID()}"
    }

    @Serializable
    data class Warning(override val text: String, override val timestamp: Timestamp = Timestamp.now()) : OutputMessage {
        override val id = "${UUID.randomUUID()}"
    }

    @Serializable
    data class Error(override val text: String, override val timestamp: Timestamp = Timestamp.now()) : OutputMessage {
        override val id = "${UUID.randomUUID()}"
    }

    @Serializable
    data class Success(override val text: String, override val timestamp: Timestamp = Timestamp.now()) : OutputMessage {
        override val id = "${UUID.randomUUID()}"
    }
}