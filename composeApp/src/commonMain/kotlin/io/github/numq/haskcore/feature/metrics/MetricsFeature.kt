package io.github.numq.haskcore.feature.metrics

import io.github.numq.haskcore.feature.Feature
import kotlin.time.TimeSource

internal class MetricsFeature<Command, State>(
    private val feature: Feature<Command, State>, private val metricsCollector: MetricsCollector<Command>
) : Feature<Command, State> by feature {
    override suspend fun execute(command: Command) {
        val startTime = TimeSource.Monotonic.markNow()

        try {
            feature.execute(command)

            metricsCollector.recordSuccess(command = command, duration = startTime.elapsedNow())
        } catch (throwable: Throwable) {
            metricsCollector.recordFailure(command = command, duration = startTime.elapsedNow(), throwable = throwable)

            throw throwable
        }
    }
}