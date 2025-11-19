package io.github.numq.haskcore.explorer

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

internal interface ExplorerSnapshotDataSource {
    val explorerSnapshot: Flow<ExplorerSnapshot>

    suspend fun update(transform: (ExplorerSnapshot) -> ExplorerSnapshot): Result<ExplorerSnapshot>

    class Default(private val dataStore: DataStore<ExplorerSnapshot>) : ExplorerSnapshotDataSource {
        override val explorerSnapshot = dataStore.data

        override suspend fun update(transform: (ExplorerSnapshot) -> ExplorerSnapshot) = runCatching {
            dataStore.updateData(transform)
        }
    }
}