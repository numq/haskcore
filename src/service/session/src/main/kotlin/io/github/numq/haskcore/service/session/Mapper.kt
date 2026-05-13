package io.github.numq.haskcore.service.session

import io.github.numq.haskcore.common.core.timestamp.Timestamp

internal fun SessionRecordData.toSessionRecord() = SessionRecord(
    path = path, name = name, timestamp = Timestamp(nanoseconds = timestampNanos)
)

internal fun SessionRecord.toSessionRecordData() = SessionRecordData(
    path = path, name = name, timestampNanos = timestamp.nanoseconds
)

internal fun SessionData.toSession() =
    Session(history = history.values.sortedByDescending(SessionRecordData::timestampNanos).map { sessionRecordData ->
        sessionRecordData.toSessionRecord()
    }, active = active.sortedByDescending(SessionRecordData::timestampNanos).map { sessionRecordData ->
        sessionRecordData.toSessionRecord()
    })