package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import io.github.numq.haskcore.feature.execution.core.LaunchTarget
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService

class BuildConfiguration(private val runtimeService: RuntimeService) : UseCase.Command<BuildConfiguration.Input> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input.configuration) {
        val request = when (val launchTarget = target) {
            is LaunchTarget.Stack -> RuntimeRequest.Stack(
                id = "build-$id",
                name = "Build $name",
                arguments = listOf("build", launchTarget.componentName),
                workingDir = launchTarget.workingDir,
                env = env
            )

            is LaunchTarget.Cabal -> RuntimeRequest.Cabal(
                id = "build-$id",
                name = "Build $name",
                arguments = listOf("build", launchTarget.componentName),
                workingDir = launchTarget.workingDir,
                env = env
            )

            is LaunchTarget.File -> RuntimeRequest.Ghc(
                id = "build-$id",
                name = "Check $name",
                arguments = listOf("-fno-code", "-fforce-recomp", launchTarget.filePath),
                workingDir = launchTarget.workingDir,
                env = env
            )
        }

        runtimeService.start(request = request).bind()
    }
}