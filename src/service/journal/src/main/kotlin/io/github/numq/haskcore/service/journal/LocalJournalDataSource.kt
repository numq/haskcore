package io.github.numq.haskcore.service.journal

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalJournalDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<JournalData>
) : JournalDataSource {
    override val journalData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (JournalData) -> JournalData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}