package io.github.numq.haskcore.timestamp

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
internal data class Timestamp(val millis: Long) : Comparable<Timestamp> {
    companion object {
        fun now() = Timestamp(millis = System.currentTimeMillis())
    }

    val milliseconds = millis.milliseconds

    override fun compareTo(other: Timestamp) = milliseconds.compareTo(other.milliseconds)
}