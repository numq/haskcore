package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.Explorer
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveExplorer(
    private val explorerRepository: ExplorerRepository,
) : UseCase<Unit, Flow<Explorer>> {
    override suspend fun execute(input: Unit) = Result.success(explorerRepository.explorer)
}