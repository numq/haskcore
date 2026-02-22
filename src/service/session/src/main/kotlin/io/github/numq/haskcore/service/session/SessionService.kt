package io.github.numq.haskcore.service.session

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface SessionService : AutoCloseable {
    val session: Flow<Session>

    suspend fun openSessionRecord(path: String, name: String?): Either<Throwable, Unit>

    suspend fun updateSessionRecord(path: String, name: String?): Either<Throwable, Unit>

    suspend fun closeSessionRecord(path: String): Either<Throwable, Unit>

    suspend fun removeSessionRecordFromHistory(path: String): Either<Throwable, Unit>
}