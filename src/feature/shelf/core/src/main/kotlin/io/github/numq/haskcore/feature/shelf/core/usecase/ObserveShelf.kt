package io.github.numq.haskcore.feature.shelf.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.shelf.core.Shelf
import io.github.numq.haskcore.feature.shelf.core.ShelfService
import kotlinx.coroutines.flow.Flow

class ObserveShelf(private val shelfService: ShelfService) : UseCase<Unit, Flow<Shelf>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = shelfService.shelf
}