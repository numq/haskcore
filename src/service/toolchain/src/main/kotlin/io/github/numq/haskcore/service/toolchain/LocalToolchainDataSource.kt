package io.github.numq.haskcore.service.toolchain

import androidx.datastore.core.DataStore
import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

internal class LocalToolchainDataSource(
    private val scope: CoroutineScope, private val dataStore: DataStore<ToolchainData>
) : ToolchainDataSource {
    override val toolchain = dataStore.data

    override suspend fun get() = Either.catch { dataStore.data.first() }

    override suspend fun update(transform: (ToolchainData) -> ToolchainData) = Either.catch {
        dataStore.updateData(transform)
    }

    override fun close() {
        scope.cancel()
    }
}