package io.github.numq.haskcore.service.runtime

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlin.time.Duration

sealed interface RuntimeEvent {
    val request: RuntimeRequest

    data class Started(override val request: RuntimeRequest) : RuntimeEvent

    data class Stdout(override val request: RuntimeRequest, val text: String, val timestamp: Timestamp) : RuntimeEvent

    data class Stderr(override val request: RuntimeRequest, val text: String, val timestamp: Timestamp) : RuntimeEvent

    data class Terminated(
        override val request: RuntimeRequest, val exitCode: Int, val duration: Duration
    ) : RuntimeEvent
}