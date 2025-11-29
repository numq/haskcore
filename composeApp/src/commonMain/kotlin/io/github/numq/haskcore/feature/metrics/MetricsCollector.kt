package io.github.numq.haskcore.feature.metrics

import kotlin.time.Duration

internal interface MetricsCollector<in Command> {
    fun recordSuccess(command: Command, duration: Duration)

    fun recordFailure(command: Command, duration: Duration, throwable: Throwable)
}