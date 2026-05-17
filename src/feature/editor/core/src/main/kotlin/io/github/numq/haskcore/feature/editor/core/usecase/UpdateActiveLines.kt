package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService

class UpdateActiveLines(private val editorService: EditorService) : UseCase.Command<UpdateActiveLines.Input> {
    data class Input(val start: Int, val end: Int)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        editorService.updateActiveLines(start = start, end = end).bind()
    }
}