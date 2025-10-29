package io.github.numq.haskcore.explorer.usecase

import io.github.numq.haskcore.explorer.ExplorerRepository
import io.github.numq.haskcore.usecase.UseCase

internal class OpenExplorer(
    private val explorerRepository: ExplorerRepository,
) : UseCase<OpenExplorer.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = explorerRepository.openExplorer(path = input.path)
}