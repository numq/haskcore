package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

class CopyNodes(private val explorerRepository: ExplorerRepository) : UseCase<CopyNodes.Input, Unit> {
    data class Input(val nodes: List<ExplorerNode>)

    override suspend fun execute(input: Input) = explorerRepository.copyNodes(nodes = input.nodes)
}