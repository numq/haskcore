package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface ToolchainDataSource : AutoCloseable {
    val toolchain: Flow<ToolchainData>

    suspend fun get(): Either<Throwable, ToolchainData>

    suspend fun update(transform: (ToolchainData) -> ToolchainData): Either<Throwable, ToolchainData>
}