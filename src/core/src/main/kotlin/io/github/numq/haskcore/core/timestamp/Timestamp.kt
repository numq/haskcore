package io.github.numq.haskcore.core.timestamp

@JvmInline
value class Timestamp(val milliseconds: Long) : Comparable<Timestamp> {
    companion object {
        fun now() = Timestamp(milliseconds = System.currentTimeMillis())
    }

    override fun compareTo(other: Timestamp) = milliseconds.compareTo(other.milliseconds)
}