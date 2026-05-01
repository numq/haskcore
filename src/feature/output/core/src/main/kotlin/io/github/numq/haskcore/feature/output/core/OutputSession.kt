package io.github.numq.haskcore.feature.output.core

import kotlin.time.Duration

sealed interface OutputSession {
    val id: String

    val name: String

    val configuration: String

    val lines: List<OutputLine>

    data class Active(
        override val id: String,
        override val name: String,
        override val configuration: String,
        override val lines: List<OutputLine>,
    ) : OutputSession

    data class Completed(
        override val id: String,
        override val name: String,
        override val configuration: String,
        override val lines: List<OutputLine>,
        val exitCode: Int,
        val duration: Duration,
    ) : OutputSession
}