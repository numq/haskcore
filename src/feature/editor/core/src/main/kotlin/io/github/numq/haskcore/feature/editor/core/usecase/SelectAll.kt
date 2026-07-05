package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.text.TextService

class SelectAll(private val editorService: EditorService, private val textService: TextService) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() {
        val snapshot = textService.snapshot.value ?: return

        editorService.selectAll(snapshot = snapshot).bind()
    }
}