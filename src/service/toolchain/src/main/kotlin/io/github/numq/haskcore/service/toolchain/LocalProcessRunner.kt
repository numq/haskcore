package io.github.numq.haskcore.service.toolchain

import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.util.concurrent.TimeoutException

internal class LocalProcessRunner : ProcessRunner {
    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }

    override suspend fun runCommand(path: String, vararg args: String) = either {
        withTimeoutOrNull(TIMEOUT_MILLIS) {
            withContext(Dispatchers.IO) {
                val process = try {
                    ProcessBuilder(path, *args).redirectErrorStream(true).start()
                } catch (throwable: Throwable) {
                    raise(throwable)
                }

                try {
                    val output = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()

                    val exitCode = process.waitFor()

                    when (exitCode) {
                        0 if output.isNotEmpty() -> output.lines().firstOrNull()?.trim() ?: output

                        else -> {
                            val errorDescription = output.ifEmpty { "No output from process" }

                            raise(IllegalStateException("Execution failed (exit $exitCode): $errorDescription"))
                        }
                    }
                } catch (throwable: Throwable) {
                    process.destroyForcibly()

                    raise(throwable)
                }
            }
        } ?: raise(TimeoutException("Process $path timed out after ${TIMEOUT_MILLIS}ms"))
    }
}