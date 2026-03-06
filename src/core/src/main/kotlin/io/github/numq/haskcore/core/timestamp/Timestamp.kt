package io.github.numq.haskcore.core.timestamp

@JvmInline
value class Timestamp(val nanoseconds: Long) : Comparable<Timestamp> {
    companion object {
        fun now() = Timestamp(nanoseconds = System.nanoTime())
    }

    operator fun minus(other: Timestamp) = nanoseconds - other.nanoseconds

    override fun compareTo(other: Timestamp) = nanoseconds.compareTo(other.nanoseconds)
}