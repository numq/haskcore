package io.github.numq.haskcore.service.session

internal fun SessionData.toSession() = Session(
    history = history.values.map { sessionRecordData -> sessionRecordData.toSessionRecord() }.toSet(),
    active = active.map { sessionRecordData -> sessionRecordData.toSessionRecord() }.toSet()
)