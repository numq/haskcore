package io.github.numq.haskcore.session

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

internal interface SessionDataSource {
    val session: Flow<Session>

    suspend fun update(session: Session): Result<Unit>

    class Default(private val dataStore: DataStore<Session>) : SessionDataSource {
        override val session = dataStore.data

        override suspend fun update(session: Session) = runCatching {
            dataStore.updateData { session }

            Unit
        }
    }
}