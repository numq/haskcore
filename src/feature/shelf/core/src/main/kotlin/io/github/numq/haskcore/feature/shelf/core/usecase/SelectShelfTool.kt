package io.github.numq.haskcore.feature.shelf.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.shelf.core.ShelfService
import io.github.numq.haskcore.feature.shelf.core.ShelfTool

class SelectShelfTool(private val shelfService: ShelfService) : UseCase<SelectShelfTool.Input, Unit> {
    data class Input(val tool: ShelfTool)

    override suspend fun Raise<Throwable>.execute(input: Input) = shelfService.selectShelfTool(tool = input.tool).bind()
}