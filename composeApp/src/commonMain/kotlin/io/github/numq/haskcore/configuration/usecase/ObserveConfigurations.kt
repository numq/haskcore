package io.github.numq.haskcore.configuration.usecase

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.ConfigurationRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveConfigurations(
    private val configurationRepository: ConfigurationRepository
) : UseCase<Unit, Flow<List<Configuration>>> {
    override suspend fun execute(input: Unit) = Result.success(configurationRepository.configurations)
}