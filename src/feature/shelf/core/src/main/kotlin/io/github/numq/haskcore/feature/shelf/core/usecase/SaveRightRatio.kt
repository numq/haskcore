package io.github.numq.haskcore.feature.shelf.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.shelf.core.ShelfService

class SaveRightRatio(private val shelfService: ShelfService) : UseCase<SaveRightRatio.Input, Unit> {
    data class Input(val ratio: Float)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        shelfService.updateRightRatio(ratio = input.ratio).bind()
}