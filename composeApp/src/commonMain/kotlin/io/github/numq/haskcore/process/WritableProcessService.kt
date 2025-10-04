package io.github.numq.haskcore.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File

interface WritableProcessService : ProcessService {
    suspend fun <T> useProcess(
        commands: List<String>,
        workingDirectory: String,
        environment: Map<String, String> = emptyMap(),
        block: suspend (Process) -> T
    ): Result<T>

    class DefaultWritableProcessService(
        private val processService: ProcessService
    ) : WritableProcessService, ProcessService by processService {
        override suspend fun <T> useProcess(
            commands: List<String>,
            workingDirectory: String,
            environment: Map<String, String>,
            block: suspend (Process) -> T
        ) = runCatching {
            val process = runInterruptible(Dispatchers.IO) {
                ProcessBuilder(commands).directory(File(workingDirectory)).apply {
                    environment().putAll(environment)
                }.redirectErrorStream(true).start()
            }

            try {
                block(process)
            } finally {
                process.destroy()
            }
        }
    }
}