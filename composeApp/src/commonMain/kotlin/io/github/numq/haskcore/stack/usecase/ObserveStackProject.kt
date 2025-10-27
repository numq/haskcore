package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.stack.StackProject
import io.github.numq.haskcore.stack.StackRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveStackProject(
    private val stackRepository: StackRepository
) : UseCase<Unit, Flow<StackProject?>> {
    override suspend fun execute(input: Unit) = Result.success(stackRepository.project)
}