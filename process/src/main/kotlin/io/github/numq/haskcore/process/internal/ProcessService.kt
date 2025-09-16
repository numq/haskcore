package io.github.numq.haskcore.process.internal

import io.github.numq.haskcore.process.ProcessOutputChunk
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.time.TimeSource

interface ProcessService {
    fun execute(
        commands: List<String>, workingDirectory: String, environment: Map<String, String> = emptyMap()
    ): Flow<ProcessOutputChunk>

    class Default : ProcessService {
        override fun execute(
            commands: List<String>, workingDirectory: String, environment: Map<String, String>
        ) = flow {
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