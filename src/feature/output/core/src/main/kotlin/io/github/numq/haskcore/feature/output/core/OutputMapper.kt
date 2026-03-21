package io.github.numq.haskcore.feature.output.core

import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlin.time.Duration.Companion.nanoseconds

internal fun OutputLineData.toOutputLine() = when (this) {
    is OutputLineData.System -> OutputLine.System(
        id = id, text = text, timestamp = Timestamp(nanoseconds = timestampNanos)
    )

    is OutputLineData.Normal -> OutputLine.Normal(
        id = id, text = text, timestamp = Timestamp(nanoseconds = timestampNanos)
    )

    is OutputLineData.Error -> OutputLine.Error(
        id = id, text = text, timestamp = Timestamp(nanoseconds = timestampNanos)
    )
}

internal fun OutputLine.toOutputLineData() = when (this) {
    is OutputLine.System -> OutputLineData.System(id = id, text = text, timestampNanos = timestamp.nanoseconds)

    is OutputLine.Normal -> OutputLineData.Normal(id = id, text = text, timestampNanos = timestamp.nanoseconds)

    is OutputLine.Error -> OutputLineData.Error(id = id, text = text, timestampNanos = timestamp.nanoseconds)
}

internal fun OutputSessionData.toOutputSession() = when (this) {
    is OutputSessionData.Active -> OutputSession.Active(
        id = id, name = name, configuration = configuration, lines = lines.map(
            OutputLineData::toOutputLine
        )
    )

    is OutputSessionData.Completed -> OutputSession.Completed(
        id = id, name = name, configuration = configuration, lines = lines.map(
            OutputLineData::toOutputLine
        ), exitCode = exitCode, duration = durationNanos.nanoseconds
    )
}

internal fun OutputData.toOutput() = Output(
    sessions = sessions.map(OutputSessionData::toOutputSession), activeSession = activeSession?.toOutputSession()
)