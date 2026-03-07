package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import kotlinx.coroutines.flow.Flow

class ObserveCaret(private val editorService: EditorService) : UseCase<Unit, Flow<Caret>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = editorService.caret
}