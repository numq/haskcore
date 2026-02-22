package io.github.numq.haskcore.service.session

import io.github.numq.haskcore.core.timestamp.Timestamp

internal fun SessionRecordData.toSessionRecord() = SessionRecord(
    path = path, name = name, timestamp = Timestamp(nanoseconds = timestampNanos)
)

internal fun SessionRecord.toSessionRecordData() = SessionRecordData(
    path = path, name = name, timestampNanos = timestamp.nanoseconds
)