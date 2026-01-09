package io.github.numq.haskcore.toolchain

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

internal interface ToolchainProtoDataSource {
    val toolchainProto: Flow<ToolchainProto>

    suspend fun update(transform: (ToolchainProto) -> ToolchainProto): Result<ToolchainProto>

    class Default(private val dataStore: DataStore<ToolchainProto>) : ToolchainProtoDataSource {
        override val toolchainProto = dataStore.data

        override suspend fun update(transform: (ToolchainProto) -> ToolchainProto) = runCatching {
            dataStore.updateData(transform)
        }
    }
}