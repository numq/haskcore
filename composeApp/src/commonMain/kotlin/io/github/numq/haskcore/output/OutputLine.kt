package io.github.numq.haskcore.output

import java.util.*

internal data class OutputLine(val text: String) {
    val key: String get() = UUID.randomUUID().toString()
}