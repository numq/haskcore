package io.github.numq.haskcore.feature.output.core

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalOutputDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<OutputData>,
) : OutputDataSource {
    override val outputData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (OutputData) -> OutputData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}