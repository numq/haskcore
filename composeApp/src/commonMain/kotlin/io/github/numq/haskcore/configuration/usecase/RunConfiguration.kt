package io.github.numq.haskcore.configuration.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.output.Output
import io.github.numq.haskcore.output.toOutputMessage
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RunConfiguration(
    private val buildSystemRepository: BuildSystemRepository, private val outputRepository: OutputRepository
) : UseCase<RunConfiguration.Input, Flow<Unit>> {
    data class Input(val configuration: Configuration)

    override suspend fun execute(input: Input) = input.runCatching {
        val output = Output(name = configuration.name)

        outputRepository.open(output = output).getOrThrow()

        buildSystemRepository.execute(command = configuration.command).getOrThrow().map { buildOutput ->
            val outputMessage = buildOutput.toOutputMessage()

            outputRepository.addMessage(
                outputId = output.id, outputMessage = outputMessage
            ).getOrThrow()
        }
    }
}