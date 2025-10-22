package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.Explorer
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class GetExplorer(
    private val explorerRepository: ExplorerRepository,
) : UseCase<GetExplorer.Input, Flow<Explorer?>> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = Result.success(explorerRepository.explorer)
}
