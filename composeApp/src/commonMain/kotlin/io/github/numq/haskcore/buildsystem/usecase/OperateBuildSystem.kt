package io.github.numq.haskcore.buildsystem.usecase

import io.github.numq.haskcore.buildsystem.BuildSystemArtifact
import io.github.numq.haskcore.buildsystem.BuildSystemRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class OperateBuildSystem(
    private val buildSystemRepository: BuildSystemRepository
) : UseCase<OperateBuildSystem.Input, Flow<Unit>> {
    sealed interface Input {
        data class Build(val target: BuildSystemArtifact) : Input

        data class Run(val target: BuildSystemArtifact) : Input

        data class Test(val target: BuildSystemArtifact) : Input

        data class Clean(val target: BuildSystemArtifact) : Input

        data class CompileFile(val file: BuildSystemArtifact.HaskellFile) : Input

        data class RunFile(val file: BuildSystemArtifact.HaskellFile) : Input

        data class RunScript(val script: BuildSystemArtifact.LiterateScript) : Input
    }

    override suspend fun execute(input: Input) = with(input) {
        when (this) {
            is Input.Build -> buildSystemRepository.build(target = target)

            is Input.Run -> buildSystemRepository.run(target = target)

            is Input.Test -> buildSystemRepository.test(target = target)

            is Input.Clean -> buildSystemRepository.clean(target = target)

            is Input.CompileFile -> buildSystemRepository.compileFile(file = file)

            is Input.RunFile -> buildSystemRepository.compileFile(file = file)

            is Input.RunScript -> buildSystemRepository.runScript(script = script)
        }
    }
}