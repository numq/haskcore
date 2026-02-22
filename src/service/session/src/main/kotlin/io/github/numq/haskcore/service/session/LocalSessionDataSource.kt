package io.github.numq.haskcore.service.session

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalSessionDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<SessionData>
) : SessionDataSource {
    override val sessionData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (SessionData) -> SessionData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}