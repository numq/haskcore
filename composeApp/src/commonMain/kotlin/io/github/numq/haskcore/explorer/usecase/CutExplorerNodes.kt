package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class CutExplorerNodes(private val explorerRepository: ExplorerRepository) : UseCase<CutExplorerNodes.Input, Unit> {
    data class Input(val nodes: Set<ExplorerNode>)

    override suspend fun execute(input: Input) = explorerRepository.cutNodes(nodes = input.nodes)
}