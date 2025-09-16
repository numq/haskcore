package io.github.numq.haskcore.process

import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.process.internal.ProcessService
import kotlinx.coroutines.flow.Flow

class ExecuteProcess(
    private val processService: ProcessService
) : UseCase<ExecuteProcess.Input, Flow<ProcessOutputChunk>> {
    data class Input(
        val commands: List<String>, val workingDirectory: String, val environment: Map<String, String> = emptyMap()
    )

    override suspend fun execute(input: Input) = with(input) {
        Result.success(
            processService.execute(
                commands = commands, workingDirectory = workingDirectory, environment = environment
            )
        )
    }
}