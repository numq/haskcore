package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

class GetNodes(
    private val explorerRepository: ExplorerRepository
) : UseCase<GetNodes.Input, Flow<List<ExplorerNode>>> {
    data class Input(val rootPath: String)

    override suspend fun execute(input: Input) = explorerRepository.getNodes(rootPath = input.rootPath.trim())
}