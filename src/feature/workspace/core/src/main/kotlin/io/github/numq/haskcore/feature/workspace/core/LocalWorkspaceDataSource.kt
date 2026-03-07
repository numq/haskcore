package io.github.numq.haskcore.feature.workspace.core

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalWorkspaceDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<WorkspaceData>
) : WorkspaceDataSource {
    override val workspaceData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (WorkspaceData) -> WorkspaceData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}