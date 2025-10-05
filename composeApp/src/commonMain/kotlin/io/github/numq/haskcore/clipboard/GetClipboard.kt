package io.github.numq.haskcore.clipboard

import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class GetClipboard(private val clipboardService: ClipboardService) : UseCase<Unit, Flow<Clipboard>> {
    override suspend fun execute(input: Unit) = Result.success(clipboardService.clipboard)
}