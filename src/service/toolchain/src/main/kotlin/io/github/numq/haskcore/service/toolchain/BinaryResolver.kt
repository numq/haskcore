package io.github.numq.haskcore.service.toolchain

import arrow.core.Either

internal interface BinaryResolver {
    suspend fun findBinary(name: String, vararg paths: String): Either<Throwable, String?>
}