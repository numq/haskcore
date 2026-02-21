package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

internal interface VfsDataSource {
    suspend fun watch(path: String): Either<Throwable, Flow<VfsEvent>>

    suspend fun list(path: String): Either<Throwable, List<VirtualFile>>

    suspend fun create(path: String, isDirectory: Boolean): Either<Throwable, Unit>

    suspend fun move(src: String, dst: String, overwrite: Boolean): Either<Throwable, Unit>

    suspend fun copy(src: String, dst: String, overwrite: Boolean): Either<Throwable, Unit>

    suspend fun delete(path: String): Either<Throwable, Unit>
}