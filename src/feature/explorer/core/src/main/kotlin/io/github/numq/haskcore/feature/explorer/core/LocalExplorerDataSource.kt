package io.github.numq.haskcore.feature.explorer.core

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalExplorerDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ExplorerData>,
) : ExplorerDataSource {
    override val explorerData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ExplorerData) -> ExplorerData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}