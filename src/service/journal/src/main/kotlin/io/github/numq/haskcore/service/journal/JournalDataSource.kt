package io.github.numq.haskcore.service.journal

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface JournalDataSource : AutoCloseable {
    val journalData: Flow<JournalData>

    suspend fun get(): Either<Throwable, JournalData>

    suspend fun update(transform: (JournalData) -> JournalData): Either<Throwable, JournalData>
}