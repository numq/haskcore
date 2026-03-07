package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService

class RequestHighlightingUpdate(
    private val editorService: EditorService
) : UseCase<RequestHighlightingUpdate.Input, Unit> {
    data class Input(val startLine: Int, val endLine: Int)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        editorService.requestHighlightingUpdate(startLine = startLine, endLine = endLine).bind()
    }
}