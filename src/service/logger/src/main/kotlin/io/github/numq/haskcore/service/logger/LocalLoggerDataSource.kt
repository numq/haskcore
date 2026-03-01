package io.github.numq.haskcore.service.logger

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalLoggerDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<List<LoggerData>>
) : LoggerDataSource {
    override val loggerData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (List<LoggerData>) -> List<LoggerData>) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}