package io.github.numq.haskcore.service.logger

import io.github.numq.haskcore.common.core.log.Log
import io.github.numq.haskcore.common.core.timestamp.Timestamp

internal fun LoggerData.toLogger(timestamp: Timestamp, timestampLabel: String) = when (this) {
    is LoggerData.Info -> Log.Info(
        id = id, projectId = projectId, message = message, timestamp = timestamp, timestampLabel = timestampLabel
    )

    is LoggerData.Warning -> Log.Warning(
        id = id, projectId = projectId, message = message, timestamp = timestamp, timestampLabel = timestampLabel
    )

    is LoggerData.Error.Handled -> Log.Error.Handled(
        id = id,
        projectId = projectId,
        message = message,
        timestamp = timestamp,
        timestampLabel = timestampLabel,
        className = className,
        stackTrace = stackTrace,
    )

    is LoggerData.Error.Internal -> Log.Error.Internal(
        id = id,
        projectId = projectId,
        message = message,
        timestamp = timestamp,
        timestampLabel = timestampLabel,
        className = className,
        stackTrace = stackTrace,
    )

    is LoggerData.Error.Critical -> Log.Error.Critical(
        id = id,
        projectId = projectId,
        message = message,
        timestamp = timestamp,
        timestampLabel = timestampLabel,
        className = className,
        stackTrace = stackTrace,
    )
}

internal fun Log.toLoggerData() = when (this) {
    is Log.Info -> LoggerData.Info(
        id = id, projectId = projectId, message = message, timestampNanos = timestamp.nanoseconds
    )

    is Log.Warning -> LoggerData.Warning(
        id = id, projectId = projectId, message = message, timestampNanos = timestamp.nanoseconds
    )

    is Log.Error.Handled -> LoggerData.Error.Handled(
        id = id,
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )

    is Log.Error.Internal -> LoggerData.Error.Internal(
        id = id,
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )

    is Log.Error.Critical -> LoggerData.Error.Critical(
        id = id,
        projectId = projectId,
        message = message,
        timestampNanos = timestamp.nanoseconds,
        className = className,
        stackTrace = stackTrace
    )
}