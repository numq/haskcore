package io.github.numq.haskcore.service.runtime

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import io.github.numq.haskcore.core.timestamp.Timestamp
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

    override suspend fun execute(request: RuntimeRequest): Either<Throwable, Flow<RuntimeEvent>> = channelFlow {
        val startTime = Timestamp.now()

        val process = runInterruptible {
            ProcessBuilder(request.command, *request.arguments.toTypedArray()).apply {
                request.workingDir?.let(::File)?.let(::directory)

                redirectErrorStream(false)
            }.start()
        }

        val stdoutJob = launch {
            process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.useLines { lines ->
                    lines.forEach { line ->
                        send(RuntimeEvent.Stdout(request = request, text = line, timestamp = Timestamp.now()))
                    }
                }
            }
        }

        val stderrJob = launch {
            process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.useLines { lines ->
                    lines.forEach { line ->
                        send(RuntimeEvent.Stderr(request = request, text = line, timestamp = Timestamp.now()))
                    }
                }
            }
        }

        launch {
            send(RuntimeEvent.Started(request = request))

            val exitCode = process.waitFor()

            joinAll(stdoutJob, stderrJob)

            val duration = (Timestamp.now() - startTime).nanoseconds

            send(RuntimeEvent.Terminated(request = request, exitCode = exitCode, duration = duration))

            close()
        }

        awaitClose {
            process.destroyForcibly()
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