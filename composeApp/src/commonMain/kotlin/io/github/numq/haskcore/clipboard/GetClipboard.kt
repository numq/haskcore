package io.github.numq.haskcore.clipboard

import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class GetClipboard(private val clipboardRepository: ClipboardRepository) : UseCase<Unit, Flow<Clipboard>> {
    override suspend fun execute(input: Unit) = Result.success(clipboardRepository.clipboard)
}