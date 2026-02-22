package io.github.numq.haskcore.service.session

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface SessionDataSource : AutoCloseable {
    val sessionData: Flow<SessionData>

    suspend fun get(): Either<Throwable, SessionData>

    suspend fun update(transform: (SessionData) -> SessionData): Either<Throwable, SessionData>
}