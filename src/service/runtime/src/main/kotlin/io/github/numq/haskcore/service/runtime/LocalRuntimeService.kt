package io.github.numq.haskcore.service.runtime

import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.File
import kotlin.time.Duration.Companion.nanoseconds

internal class LocalRuntimeService : RuntimeService {
    override suspend fun execute(request: RuntimeRequest) = Either.catch {
        val startTime = Timestamp.now()

        val process = runInterruptible {
            ProcessBuilder(request.command, *request.arguments.toTypedArray()).apply {
                request.workingDir?.let(::File)?.let(::directory)

                redirectErrorStream(false)
            }.start()
        }

        callbackFlow {
            val stdoutJob = launch(Dispatchers.IO) {
                process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            trySend(RuntimeEvent.Stdout(text = line, timestamp = Timestamp.now()))
                        }
                    }
                }
            }

            val stderrJob = launch(Dispatchers.IO) {
                process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            trySend(RuntimeEvent.Stderr(text = line, timestamp = Timestamp.now()))
                        }
                    }
                }
            }

            val terminationJob = launch(Dispatchers.IO) {
                try {
                    val exitCode = process.waitFor()

                    joinAll(stdoutJob, stderrJob)

                    val duration = (Timestamp.now() - startTime).nanoseconds

                    trySend(RuntimeEvent.Terminated(exitCode = exitCode, duration = duration))
                } catch (e: Exception) {
                    close(e)
                } finally {
                    close()
                }
            }

            awaitClose {
                if (process.isAlive) {
                    process.destroyForcibly()
                }

                stdoutJob.cancel()

                stderrJob.cancel()

                terminationJob.cancel()
            }
        }
    }
}