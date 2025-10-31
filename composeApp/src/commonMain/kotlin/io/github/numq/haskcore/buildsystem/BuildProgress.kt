package io.github.numq.haskcore.buildsystem

internal sealed interface BuildProgress {
    data object Active : BuildProgress

    data object Complete : BuildProgress

    data object Idle : BuildProgress
}
