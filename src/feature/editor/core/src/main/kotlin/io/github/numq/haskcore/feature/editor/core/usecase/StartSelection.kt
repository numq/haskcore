package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.text.TextService

class StartSelection(
    private val editorService: EditorService, private val textService: TextService
) : UseCase<StartSelection.Input, Unit> {
    data class Input(val position: TextPosition)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        val snapshot = textService.snapshot.value ?: return

        editorService.startSelection(snapshot = snapshot, position = position).bind()
    }
}