package io.github.numq.haskcore.process

import kotlin.time.Duration

sealed interface ProcessOutputChunk {
    data class Line(val text: String) : ProcessOutputChunk

    data class Completed(val exitCode: Int, val duration: Duration) : ProcessOutputChunk
}