package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ToolchainService : AutoCloseable {
    val toolchain: StateFlow<Toolchain>

    suspend fun updatePaths(
        ghcPath: String?, cabalPath: String?, stackPath: String?, hlsPath: String?
    ): Either<Throwable, Unit>
}