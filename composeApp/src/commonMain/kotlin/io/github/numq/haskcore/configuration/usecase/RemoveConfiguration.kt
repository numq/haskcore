package io.github.numq.haskcore.configuration.usecase

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.ConfigurationRepository
import io.github.numq.haskcore.usecase.UseCase

internal class RemoveConfiguration(
    private val configurationRepository: ConfigurationRepository
) : UseCase<RemoveConfiguration.Input, Unit> {
    data class Input(val configuration: Configuration)

    override suspend fun execute(input: Input) =
        configurationRepository.removeConfiguration(configuration = input.configuration)
}