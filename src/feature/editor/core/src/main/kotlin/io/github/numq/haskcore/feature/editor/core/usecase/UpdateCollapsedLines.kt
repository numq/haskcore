package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService

class UpdateCollapsedLines(private val editorService: EditorService) : UseCase.Command<UpdateCollapsedLines.Input> {
    data class Input(val lines: Set<Int>)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        editorService.updateCollapsedLines(lines = lines).bind()
    }
}