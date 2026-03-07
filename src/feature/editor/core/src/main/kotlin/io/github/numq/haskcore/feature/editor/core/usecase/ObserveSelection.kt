package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import kotlinx.coroutines.flow.Flow

class ObserveSelection(private val editorService: EditorService) : UseCase<Unit, Flow<Selection>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = editorService.selection
}