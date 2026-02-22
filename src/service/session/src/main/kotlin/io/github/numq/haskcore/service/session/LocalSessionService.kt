package io.github.numq.haskcore.service.session

import arrow.core.raise.either
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LocalSessionService(
    private val scope: CoroutineScope, private val sessionDataSource: SessionDataSource
) : SessionService {
    private companion object {
        const val HISTORY_LIMIT = 30
    }

    override val session = sessionDataSource.sessionData.map(SessionData::toSession).stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Session()
    )

    override suspend fun openSessionRecord(path: String, name: String?) = either {
        sessionDataSource.update { sessionData ->
            val currentHistory = sessionData.history

            val targetRecord = currentHistory[path]?.let { existing ->
                when (existing.name) {
                    name -> existing

                    else -> existing.copy(name = name, timestampNanos = Timestamp.now().nanoseconds)
                }
            } ?: SessionRecord(path = path, name = name, timestamp = Timestamp.now()).toSessionRecordData()

            val history = (mapOf(path to targetRecord) + (currentHistory - path)).toList().take(HISTORY_LIMIT).toMap()

            val active = sessionData.active.filterNot { sessionRecordData ->
                sessionRecordData.path == path
            } + targetRecord

            sessionData.copy(history = history, active = active)
        }.bind()
    }.map { }

    override suspend fun updateSessionRecord(
        path: String, name: String?
    ) = sessionDataSource.update { sessionData ->
        val timestampNanos = Timestamp.now().nanoseconds

        val update = { record: SessionRecordData ->
            record.copy(name = name, timestampNanos = timestampNanos)
        }

        sessionData.copy(history = sessionData.history[path]?.let {
            sessionData.history + (path to update(it))
        } ?: sessionData.history, active = sessionData.active.map { sessionRecordData ->
            when (sessionRecordData.path) {
                path -> update(sessionRecordData)

                else -> sessionRecordData
            }
        })
    }.map { }

    override suspend fun closeSessionRecord(path: String) = sessionDataSource.update { sessionData ->
        sessionData.copy(active = sessionData.active.filterNot { sessionRecordData ->
            sessionRecordData.path == path
        })
    }.map { }

    override suspend fun removeSessionRecordFromHistory(path: String) = sessionDataSource.update { sessionData ->
        sessionData.copy(history = sessionData.history - path)
    }.map { }

    override fun close() {
        scope.cancel()
    }
}