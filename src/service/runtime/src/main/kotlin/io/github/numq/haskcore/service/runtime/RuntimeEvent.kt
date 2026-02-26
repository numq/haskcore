package io.github.numq.haskcore.service.runtime

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlin.time.Duration

sealed interface RuntimeEvent {
    data class Stdout(val text: String, val timestamp: Timestamp) : RuntimeEvent

    data class Stderr(val text: String, val timestamp: Timestamp) : RuntimeEvent

    data class Terminated(val exitCode: Int, val duration: Duration) : RuntimeEvent
}