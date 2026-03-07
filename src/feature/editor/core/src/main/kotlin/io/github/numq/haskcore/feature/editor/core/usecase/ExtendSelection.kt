package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.flatMap
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.text.TextService

class ExtendSelection(
    private val editorService: EditorService, private val textService: TextService
) : UseCase<ExtendSelection.Input, Unit> {
    data class Input(val position: TextPosition)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        val snapshot = textService.snapshot.value ?: return

        editorService.extendSelection(snapshot = snapshot, position = position).flatMap {
            editorService.moveCaret(snapshot = snapshot, position = position)
        }.bind()
    }
}