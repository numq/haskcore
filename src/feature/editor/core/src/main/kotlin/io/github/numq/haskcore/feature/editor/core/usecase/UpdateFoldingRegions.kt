package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService

class UpdateFoldingRegions(private val editorService: EditorService) : UseCase.Command<UpdateFoldingRegions.Input> {
    data class Input(val ranges: List<IntRange>)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        editorService.updateFoldingRegions(ranges = ranges).bind()
    }
}