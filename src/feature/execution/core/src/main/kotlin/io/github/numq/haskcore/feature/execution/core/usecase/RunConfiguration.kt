package io.github.numq.haskcore.feature.execution.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.execution.core.BeforeRunTask
import io.github.numq.haskcore.feature.execution.core.ExecutionConfiguration
import io.github.numq.haskcore.feature.execution.core.LaunchTarget
import io.github.numq.haskcore.service.runtime.RuntimeEvent
import io.github.numq.haskcore.service.runtime.RuntimeRequest
import io.github.numq.haskcore.service.runtime.RuntimeService
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

class RunConfiguration(private val runtimeService: RuntimeService) : UseCase.Command<RunConfiguration.Input> {
    data class Input(val configuration: ExecutionConfiguration)

    override suspend fun Raise<Throwable>.command(input: Input) {
        val configuration = input.configuration

        configuration.beforeRun.filter(BeforeRunTask::isEnabled).forEach { task ->
            val taskRequest = when (task) {
                is BeforeRunTask.Build -> {
                    val buildArguments = when (val launchTarget = configuration.target) {
                        is LaunchTarget.Stack -> listOfNotNull(
                            "build", if (task.cleanFirst) "--clean" else null, launchTarget.componentName
                        )

                        is LaunchTarget.Cabal -> listOfNotNull(
                            "build", if (task.cleanFirst) "--clean" else null, launchTarget.componentName
                        )

                        is LaunchTarget.File -> listOfNotNull(
                            "-fno-code", "-fforce-recomp", launchTarget.filePath
                        )
                    }

                    when (val launchTarget = configuration.target) {
                        is LaunchTarget.Stack -> RuntimeRequest.Stack(
                            id = configuration.id,
                            name = "Building ${launchTarget.name}",
                            arguments = buildArguments,
                            workingDir = launchTarget.workingDir,
                            env = configuration.env
                        )

                        is LaunchTarget.Cabal -> RuntimeRequest.Cabal(
                            id = configuration.id,
                            name = "Building ${launchTarget.name}",
                            arguments = buildArguments,
                            workingDir = launchTarget.workingDir,
                            env = configuration.env
                        )

                        is LaunchTarget.File -> RuntimeRequest.Ghc(
                            id = configuration.id,
                            name = "Checking ${launchTarget.name}",
                            arguments = buildArguments,
                            workingDir = launchTarget.workingDir,
                            env = configuration.env
                        )
                    }
                }

                is BeforeRunTask.ExternalTool -> RuntimeRequest.Custom(
                    id = configuration.id,
                    name = "External tool: ${task.command}",
                    command = task.command,
                    arguments = task.arguments,
                    workingDir = task.workingDir ?: configuration.target.workingDir,
                    env = configuration.env
                )
            }

            val events = runtimeService.execute(request = taskRequest).bind()

            val result = events.filterIsInstance<RuntimeEvent.Terminated>().first()

            if (result.exitCode != 0) {
                raise(RuntimeException("Before Run task '${taskRequest.name}' failed with exit code ${result.exitCode}"))
            }
        }

        val runRequest = when (val launchTarget = configuration.target) {
            is LaunchTarget.Stack -> RuntimeRequest.Stack(
                id = configuration.id,
                name = configuration.name,
                arguments = listOf("run", launchTarget.componentName, "--") + configuration.programArguments,
                workingDir = launchTarget.workingDir,
                env = configuration.env
            )

            is LaunchTarget.Cabal -> RuntimeRequest.Cabal(
                id = configuration.id,
                name = configuration.name,
                arguments = listOf("run", launchTarget.componentName, "--") + configuration.programArguments,
                workingDir = launchTarget.workingDir,
                env = configuration.env
            )

            is LaunchTarget.File -> RuntimeRequest.Ghc(
                id = configuration.id,
                name = configuration.name,
                arguments = listOf(launchTarget.filePath) + configuration.programArguments,
                workingDir = launchTarget.workingDir,
                env = configuration.env
            )
        }

        runtimeService.start(request = runRequest).bind()
    }
}