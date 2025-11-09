package io.github.numq.haskcore.buildsystem

import kotlin.time.Duration

internal sealed interface BuildOutput {
    data class Line(val text: String) : BuildOutput

    data class Completion(val exitCode: Int, val duration: Duration) : BuildOutput
}