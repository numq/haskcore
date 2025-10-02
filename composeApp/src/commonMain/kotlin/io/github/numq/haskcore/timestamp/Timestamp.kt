package io.github.numq.haskcore.timestamp

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class Timestamp(val millis: Long) {
    val milliseconds = millis.milliseconds

    companion object {
        fun now() = Timestamp(System.currentTimeMillis())
    }
}