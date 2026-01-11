package io.github.numq.haskcore.output

import io.github.numq.haskcore.timestamp.Timestamp
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class Output(
    val id: String = "${UUID.randomUUID()}", val name: String, val outputMessages: List<OutputMessage> = emptyList()
) {
    val timestamp = Timestamp.now()
}