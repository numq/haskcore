package io.github.numq.haskcore.service.project

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalProjectDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ProjectData>
) : ProjectDataSource {
    override val projectData = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ProjectData) -> ProjectData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}