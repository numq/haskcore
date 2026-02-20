package io.github.numq.haskcore.service.toolchain

import arrow.core.Either

internal interface ProcessRunner {
    suspend fun runCommand(path: String, vararg args: String): Either<Throwable, String>
}