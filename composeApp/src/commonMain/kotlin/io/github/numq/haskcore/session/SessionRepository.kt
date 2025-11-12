package io.github.numq.haskcore.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.Closeable

internal interface SessionRepository : Closeable {
    val session: StateFlow<Session>

    suspend fun updateSession(session: Session): Result<Unit>

    class Default(private val sessionDataSource: SessionDataSource) : SessionRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.Default)

        override val session = sessionDataSource.session.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Lazily,
            initialValue = Session()
        )

        override suspend fun updateSession(session: Session) = sessionDataSource.update(session = session)

        override fun close() = coroutineScope.cancel()
    }
}