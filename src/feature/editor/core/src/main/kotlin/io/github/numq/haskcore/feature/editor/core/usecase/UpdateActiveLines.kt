package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService

class UpdateActiveLines(private val editorService: EditorService) : UseCase<UpdateActiveLines.Input, Unit> {
    data class Input(val start: Int, val end: Int)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        editorService.updateActiveLines(start = start, end = end).bind()
    }
}