package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.stack.StackOutput
import io.github.numq.haskcore.stack.StackService
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class CreateStackProject(
    private val stackService: StackService
) : UseCase<CreateStackProject.Input, Flow<StackOutput>> {
    data class Input(val path: String, val name: String, val template: String?)

    override suspend fun execute(input: Input) = with(input) {
        stackService.createProject(path = path, name = name, template = template)
    }
}