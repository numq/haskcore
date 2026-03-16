package io.github.numq.haskcore.core.text

@JvmInline
value class TextRevision(val value: Long) : Comparable<TextRevision> {
    companion object {
        val ZERO = TextRevision(value = 0)
    }

    override fun compareTo(other: TextRevision) = value.compareTo(other.value)
}