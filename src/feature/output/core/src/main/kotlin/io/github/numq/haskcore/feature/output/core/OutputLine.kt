package io.github.numq.haskcore.feature.output.core

import io.github.numq.haskcore.common.core.timestamp.Timestamp

sealed interface OutputLine {
    val id: String

    val text: String

    val timestamp: Timestamp

    data class System(
        override val id: String, override val text: String, override val timestamp: Timestamp,
    ) : OutputLine

    data class Normal(
        override val id: String, override val text: String, override val timestamp: Timestamp,
    ) : OutputLine

    data class Error(
        override val id: String, override val text: String, override val timestamp: Timestamp,
    ) : OutputLine
}