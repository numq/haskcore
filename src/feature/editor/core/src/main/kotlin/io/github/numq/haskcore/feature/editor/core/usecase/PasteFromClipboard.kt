package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextOperation
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.clipboard.ClipboardService
import io.github.numq.haskcore.service.text.TextService

class PasteFromClipboard(
    private val editorService: EditorService,
    private val clipboardService: ClipboardService,
    private val textService: TextService,
) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() {
        val snapshot = textService.snapshot.value ?: return

        val text = clipboardService.clipboard.value.text?.takeIf(String::isNotEmpty) ?: return

        val caret = editorService.caret.value

        val selection = editorService.selection.value

        val data = when {
            selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                range = selection.range, text = text
            )

            else -> TextOperation.Data.Single.Insert(
                position = caret.position, text = text
            )
        }

        textService.execute(operation = TextOperation.User(revision = snapshot.revision, data = data)).bind()
    }
}