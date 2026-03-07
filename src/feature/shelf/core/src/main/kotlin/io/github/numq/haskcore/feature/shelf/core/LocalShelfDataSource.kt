package io.github.numq.haskcore.feature.shelf.core

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalShelfDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ShelfData>
) : ShelfDataSource {
    override val shelfData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ShelfData) -> ShelfData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}