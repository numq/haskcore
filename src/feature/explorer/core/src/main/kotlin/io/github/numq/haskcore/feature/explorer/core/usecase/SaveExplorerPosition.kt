package io.github.numq.haskcore.feature.explorer.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.explorer.core.ExplorerPosition
import io.github.numq.haskcore.feature.explorer.core.ExplorerService

class SaveExplorerPosition(
    private val explorerService: ExplorerService,
) : UseCase<SaveExplorerPosition.Input, Unit> {
    data class Input(val position: ExplorerPosition)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        explorerService.saveExplorerPosition(position = input.position).bind()
}