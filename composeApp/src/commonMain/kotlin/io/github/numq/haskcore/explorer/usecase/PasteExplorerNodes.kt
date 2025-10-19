package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerNode
import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class PasteExplorerNodes(private val explorerRepository: ExplorerRepository) : UseCase<PasteExplorerNodes.Input, Unit> {
    data class Input(val destination: ExplorerNode)

    override suspend fun execute(input: Input) = explorerRepository.pasteNodes(destination = input.destination)
}