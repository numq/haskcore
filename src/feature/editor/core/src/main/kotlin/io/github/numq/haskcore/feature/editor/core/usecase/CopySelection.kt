package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.clipboard.ClipboardService
import io.github.numq.haskcore.service.text.TextService

class CopySelection(
    private val editorService: EditorService,
    private val clipboardService: ClipboardService,
    private val textService: TextService,
) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() {
        val snapshot = textService.snapshot.value ?: return

        val caret = editorService.caret.value

        val selection = editorService.selection.value

        val range = selection.range.takeIf(TextRange::isNotEmpty) ?: run {
            val currentLine = caret.position.line

            val lineLength = snapshot.getLineLength(line = currentLine)

            TextRange(
                start = TextPosition(line = currentLine, column = 0),
                end = TextPosition(line = currentLine, column = lineLength)
            )
        }

        val text = snapshot.getTextInRange(range = range)

        clipboardService.copyToClipboard(text = text).bind()
    }
}