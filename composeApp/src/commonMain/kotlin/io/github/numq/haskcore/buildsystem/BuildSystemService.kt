package io.github.numq.haskcore.buildsystem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

internal abstract class BuildSystemService<Command : BuildCommand> {
    fun execute(command: Command) = runCatching {
        callbackFlow {
            val startTime = System.currentTimeMillis()

            val workingDirectory = Path.of(command.path)

            val process = try {
                ProcessBuilder(command.command + command.arguments).run {
                    directory(workingDirectory.toFile())

                    redirectErrorStream(true)

                    start()
                }
            } catch (throwable: Throwable) {
                throw BuildSystemException("Failed to start stack process: ${throwable.message}")
            }

            try {
                process.inputStream.bufferedReader().use { reader ->
                    while (isActive) {
                        val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break

                        send(BuildOutput.Line(text = line))
                    }

                    val exitCode = runInterruptible(Dispatchers.IO) { process.waitFor() }

                    val duration = (System.currentTimeMillis() - startTime).milliseconds

                    send(BuildOutput.Completion(exitCode, duration))
                }
            } catch (throwable: Throwable) {
                throw BuildSystemException("Failed to read stack output: ${throwable.message}")
            } finally {
                process.destroy()
            }

            awaitClose { process.destroy() }
        }.flowOn(Dispatchers.IO)
    }
}