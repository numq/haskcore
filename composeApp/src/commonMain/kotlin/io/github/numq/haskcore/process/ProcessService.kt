package io.github.numq.haskcore.process

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.Reader
import kotlin.time.TimeSource

interface ProcessService {
    suspend fun execute(
        commands: List<String>,
        workingDirectory: String,
        environment: Map<String, String> = emptyMap(),
    ): Result<ProcessOutput>

    suspend fun stream(
        commands: List<String>,
        workingDirectory: String,
        environment: Map<String, String> = emptyMap(),
    ): Result<Flow<ProcessOutputChunk>>

    class Default : ProcessService {
        override suspend fun execute(
            commands: List<String>, workingDirectory: String, environment: Map<String, String>
        ) = runCatching {
            runInterruptible(Dispatchers.IO) {
                val process = ProcessBuilder(commands).directory(File(workingDirectory)).apply {
                    environment().putAll(environment)
                }.redirectErrorStream(true).start()

                val startTime = TimeSource.Monotonic.markNow()

                val text = process.inputStream.bufferedReader().use(Reader::readLines).joinToString()

                val exitCode = process.waitFor()

                val duration = startTime.elapsedNow()

                ProcessOutput(text = text, exitCode = exitCode, duration = duration)
            }
        }

        override suspend fun stream(
            commands: List<String>, workingDirectory: String, environment: Map<String, String>
        ) = runCatching {
            flow {
                withContext(Dispatchers.IO) {
                    val process = ProcessBuilder(commands).directory(File(workingDirectory)).apply {
                        environment().putAll(environment)
                    }.redirectErrorStream(true).start()

                    val startTime = TimeSource.Monotonic.markNow()

                    process.inputStream.bufferedReader().use { reader ->
                        while (currentCoroutineContext().isActive) {
                            when (val line = reader.readLine()) {
                                null -> break

                                else -> emit(ProcessOutputChunk.Line(text = line))
                            }
                        }
                    }

                    val exitCode = process.waitFor()

                    val duration = startTime.elapsedNow()

                    emit(ProcessOutputChunk.Completed(exitCode = exitCode, duration = duration))
                }
            }
        }
    }
}