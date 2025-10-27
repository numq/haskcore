package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.stack.StackOutputMapper
import io.github.numq.haskcore.stack.StackRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class BuildStackProject(
    private val outputRepository: OutputRepository, private val stackRepository: StackRepository
) : UseCase<BuildStackProject.Input, Flow<Unit>> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = stackRepository.build(path = input.path).mapCatching { stackOutput ->
        stackOutput.map { output ->
            outputRepository.send(message = StackOutputMapper.transform(output = output)).getOrThrow()
        }
    }
}