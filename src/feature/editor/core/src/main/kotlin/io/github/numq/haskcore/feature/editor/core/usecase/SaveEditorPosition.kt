package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorPosition
import io.github.numq.haskcore.feature.editor.core.EditorService

class SaveEditorPosition(private val editorService: EditorService) : UseCase.Command<SaveEditorPosition.Input> {
    data class Input(val position: EditorPosition)

    override suspend fun Raise<Throwable>.command(input: Input) =
        editorService.saveEditorPosition(position = input.position).bind()
}