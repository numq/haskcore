package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class CleanStackProject(
    private val stackRepository: StackRepository
) : UseCase<CleanStackProject.Input, Flow<StackOutput>> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = stackRepository.clean(path = input.path)
}