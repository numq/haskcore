package io.github.numq.haskcore.service.logger

import io.github.numq.haskcore.core.log.Log
import io.github.numq.haskcore.core.timestamp.Timestamp

internal fun LoggerData.toLog() = when (this) {
    is LoggerData.Info -> Log.Info(
        projectId = projectId, message = message, timestamp = Timestamp(nanoseconds = timestampNanos)
    )

    is LoggerData.Warning -> Log.Warning(
        projectId = projectId, message = message, timestamp = Timestamp(nanoseconds = timestampNanos)
    )

    is LoggerData.Error.Handled -> Log.Error.Handled(
        projectId = projectId,
        message = message,
        timestamp = Timestamp(nanoseconds = timestampNanos),
        className = className,
        stackTrace = stackTrace,
    )

    is LoggerData.Error.Internal -> Log.Error.Internal(
        projectId = projectId,
        message = message,
        timestamp = Timestamp(nanoseconds = timestampNanos),
        className = className,
        stackTrace = stackTrace,
    )

    is LoggerData.Error.Critical -> Log.Error.Critical(
        projectId = projectId,
        message = message,
        timestamp = Timestamp(nanoseconds = timestampNanos),
        className = className,
        stackTrace = stackTrace,
    )
}

internal fun Log.toLogData() = when (this) {
    is Log.Info -> LoggerData.Info(
        projectId = projectId, message = message, timestampNanos = timestamp.nanoseconds
    )

    is Log.Warning -> LoggerData.Warning(
        projectId = projectId, message = message, timestampNanos = timestamp.nanoseconds
    )

    is Log.Error.Handled -> LoggerData.Error.Handled(
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )

    is Log.Error.Internal -> LoggerData.Error.Internal(
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )

    is Log.Error.Critical -> LoggerData.Error.Critical(
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )
}