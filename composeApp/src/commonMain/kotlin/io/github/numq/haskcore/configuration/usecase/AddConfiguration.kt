package io.github.numq.haskcore.configuration.usecase

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.ConfigurationRepository
import io.github.numq.haskcore.usecase.UseCase

internal class AddConfiguration(
    private val configurationRepository: ConfigurationRepository
) : UseCase<AddConfiguration.Input, Configuration> {
    data class Input(val path: String, val name: String, val command: String)

    override suspend fun execute(input: Input) = with(input) {
        configurationRepository.addConfiguration(
            configuration = Configuration(
                path = path,
                name = name,
                command = command
            )
        )
    }
}