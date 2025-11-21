package io.github.numq.haskcore.session

import kotlinx.coroutines.flow.Flow

internal interface SessionRepository {
    val session: Flow<Session>

    suspend fun getSession(): Result<Session?>

    suspend fun updateSession(transform: (Session) -> Session): Result<Session>

    class Default(private val sessionDataSource: SessionDataSource) : SessionRepository {
        override val session = sessionDataSource.session

        override suspend fun getSession() = sessionDataSource.get()

        override suspend fun updateSession(transform: (Session) -> Session) = sessionDataSource.update(transform)
    }
}