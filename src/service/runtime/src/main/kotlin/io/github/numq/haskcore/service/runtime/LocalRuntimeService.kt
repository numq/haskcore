package io.github.numq.haskcore.service.runtime

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.nanoseconds

internal class LocalRuntimeService(private val scope: CoroutineScope) : RuntimeService {
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _events = MutableSharedFlow<RuntimeEvent>()

    override val events = _events.asSharedFlow()

    private fun String.removeAnsi() = replace(Regex("\u001b\\[[0-9;]*[a-zA-Z]"), "")

    override suspend fun isActive(id: String) = Either.catch {
        jobs[id]?.isActive == true
    }

    override suspend fun execute(request: RuntimeRequest): Either<Throwable, Flow<RuntimeEvent>> = channelFlow {
        val startTime = Timestamp.now()

        val process = runInterruptible {
            ProcessBuilder(request.command, *request.arguments.toTypedArray()).apply {
                request.workingDir?.let(::File)?.takeIf(File::exists)?.let(::directory)

                environment().putAll(request.env)

                redirectErrorStream(false)
            }.start()
        }

        val stdoutJob = launch {
            process.inputStream.bufferedReader().use { reader ->
                reader.useLines { lines ->
                    lines.forEach { line ->
                        send(
                            RuntimeEvent.Stdout(
                                request = request, text = line.removeAnsi(), timestamp = Timestamp.now()
                            )
                        )
                    }
                }
            }
        }

        val stderrJob = launch {
            process.errorStream.bufferedReader().use { reader ->
                reader.useLines { lines ->
                    lines.forEach { line ->
                        send(
                            RuntimeEvent.Stderr(
                                request = request, text = line.removeAnsi(), timestamp = Timestamp.now()
                            )
                        )
                    }
                }
            }
        }

        send(RuntimeEvent.Started(request = request))

        val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

        joinAll(stdoutJob, stderrJob)

        val duration = (Timestamp.now() - startTime).nanoseconds

        val terminatedEvent = RuntimeEvent.Terminated(request = request, exitCode = exitCode, duration = duration)

        trySend(terminatedEvent)

        _events.emit(terminatedEvent)

        close()

        awaitClose {
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }
    }.flowOn(Dispatchers.IO).right()

    override suspend fun start(request: RuntimeRequest) = Either.catch {
        val job = execute(request = request).getOrElse { throwable ->
            throw throwable
        }.onEach(_events::emit).launchIn(scope = scope)

        jobs.compute(request.id) { _, previousJob ->
            previousJob?.cancel()

            job
        }

        Unit
    }

    override suspend fun stop(id: String) = Either.catch {
        jobs.remove(id)?.cancel()

        Unit
    }

    override fun close() {
        scope.cancel()
    }
}