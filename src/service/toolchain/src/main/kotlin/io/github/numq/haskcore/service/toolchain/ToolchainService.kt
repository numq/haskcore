package io.github.numq.haskcore.service.toolchain

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ToolchainService : AutoCloseable {
    val toolchain: StateFlow<Toolchain>

    suspend fun resetGhcPath(): Either<Throwable, Unit>

    suspend fun resetCabalPath(): Either<Throwable, Unit>

    suspend fun resetStackPath(): Either<Throwable, Unit>

    suspend fun resetHlsPath(): Either<Throwable, Unit>

    suspend fun updateGhcPath(path: String): Either<Throwable, Unit>

    suspend fun updateCabalPath(path: String): Either<Throwable, Unit>

    suspend fun updateStackPath(path: String): Either<Throwable, Unit>

    suspend fun updateHlsPath(path: String): Either<Throwable, Unit>
}