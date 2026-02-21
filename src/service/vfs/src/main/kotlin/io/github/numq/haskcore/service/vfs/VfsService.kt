package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface VfsService : AutoCloseable {
    suspend fun observeDirectory(path: String): Either<Throwable, Flow<List<VirtualFile>>>

    suspend fun create(path: String, isDirectory: Boolean): Either<Throwable, Unit>

    suspend fun move(src: String, dst: String, overwrite: Boolean): Either<Throwable, Unit>

    suspend fun copy(src: String, dst: String, overwrite: Boolean): Either<Throwable, Unit>

    suspend fun delete(path: String): Either<Throwable, Unit>
}