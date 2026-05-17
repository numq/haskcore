package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.text.TextService

class StartSelection(
    private val editorService: EditorService, private val textService: TextService,
) : UseCase.Command<StartSelection.Input> {
    data class Input(val position: TextPosition)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        val snapshot = textService.snapshot.value ?: return

        editorService.startSelection(snapshot = snapshot, position = position).bind()
    }
}