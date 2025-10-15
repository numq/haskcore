package io.github.numq.haskcore.output

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal data class Output(val text: String) {
    val key = UUID.randomUUID().toString()
}