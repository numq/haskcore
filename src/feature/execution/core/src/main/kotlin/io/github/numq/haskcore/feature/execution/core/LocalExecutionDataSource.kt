package io.github.numq.haskcore.feature.execution.core

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalExecutionDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ExecutionData>
) : ExecutionDataSource {
    override val executionData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ExecutionData) -> ExecutionData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}