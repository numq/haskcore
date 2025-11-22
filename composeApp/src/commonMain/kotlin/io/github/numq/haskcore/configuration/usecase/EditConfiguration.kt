package io.github.numq.haskcore.configuration.usecase

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.ConfigurationRepository
import io.github.numq.haskcore.usecase.UseCase

internal class EditConfiguration(
    private val configurationRepository: ConfigurationRepository
) : UseCase<EditConfiguration.Input, Configuration> {
    data class Input(val configuration: Configuration)

    override suspend fun execute(input: Input) =
        configurationRepository.editConfiguration(configuration = input.configuration)
}