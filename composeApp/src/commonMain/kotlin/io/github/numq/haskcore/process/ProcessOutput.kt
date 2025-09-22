package io.github.numq.haskcore.process

import kotlin.time.Duration

data class ProcessOutput(val text: String, val exitCode: Int, val duration: Duration)