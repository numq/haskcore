package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class CreateStackProject(
    private val stackRepository: StackRepository
) : UseCase<CreateStackProject.Input, Flow<StackOutput>> {
    data class Input(val path: String, val name: String, val template: String)

    override suspend fun execute(input: Input) = with(input) {
        stackRepository.create(path = path, name = name, template = template)
    }
}