package io.github.numq.haskcore.session

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal interface SessionDataSource {
    val session: Flow<Session>

    suspend fun get(): Result<Session?>

    suspend fun update(transform: (Session) -> Session): Result<Session>

    class Default(private val dataStore: DataStore<Session>) : SessionDataSource {
        override val session = dataStore.data

        override suspend fun get() = runCatching { dataStore.data.firstOrNull() }

        override suspend fun update(transform: (Session) -> Session) = runCatching {
            dataStore.updateData(transform)
        }
    }
}