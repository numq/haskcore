package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import io.github.numq.haskcore.feature.execution.core.LaunchTarget
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService
import java.util.*

class RunConfiguration(private val runtimeService: RuntimeService) : UseCase.Command<RunConfiguration.Input> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input.configuration) {
        val id = "${id}_${UUID.randomUUID()}"

        val request = when (val launchTarget = target) {
            is LaunchTarget.Stack -> RuntimeRequest.Stack(
                id = id,
                name = name,
                arguments = listOf("run", launchTarget.componentName, "--") + programArguments,
                workingDir = launchTarget.workingDir,
                env = env
            )

            is LaunchTarget.Cabal -> RuntimeRequest.Cabal(
                id = id,
                name = name,
                arguments = listOf("run", launchTarget.componentName, "--") + programArguments,
                workingDir = launchTarget.workingDir,
                env = env
            )

            is LaunchTarget.File -> RuntimeRequest.Ghc(
                id = id,
                name = name,
                arguments = listOf(launchTarget.filePath) + programArguments,
                workingDir = launchTarget.workingDir,
                env = env
            )
        }

        runtimeService.start(request = request).bind()
    }
}