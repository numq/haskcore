package io.github.numq.haskcore.service.session

internal fun SessionData.toSession() =
    Session(history = history.values.sortedByDescending(SessionRecordData::timestampNanos).map { sessionRecordData ->
        sessionRecordData.toSessionRecord()
    }, active = active.sortedByDescending(SessionRecordData::timestampNanos).map { sessionRecordData ->
        sessionRecordData.toSessionRecord()
    })