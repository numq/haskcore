package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

class MoveNodes(private val explorerRepository: ExplorerRepository) : UseCase<MoveNodes.Input, Unit> {
    data class Input(val nodes: List<ExplorerNode>, val destination: ExplorerNode)

    override suspend fun execute(input: Input) = with(input) {
        explorerRepository.moveNodes(nodes = nodes, destination = destination)
    }
}
