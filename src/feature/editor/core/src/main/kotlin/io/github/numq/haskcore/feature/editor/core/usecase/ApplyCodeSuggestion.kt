package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.text.TextOperation
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRange
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.analysis.CodeSuggestion
import io.github.numq.haskcore.service.text.TextService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApplyCodeSuggestion(
    private val editorService: EditorService,
    private val textService: TextService,
) : UseCase.Command<ApplyCodeSuggestion.Input> {
    data class Input(val snapshot: TextSnapshot, val position: TextPosition, val suggestion: CodeSuggestion)

    private val mutex = Mutex()

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        mutex.withLock {
            val lineText = snapshot.getLineText(line = position.line)

            val prefixBeforeCaret = lineText.take(position.column)

            val lastNonWordCharIndex = prefixBeforeCaret.indexOfLast { char ->
                !char.isLetterOrDigit() && char != '_' && char != '\''
            }

            val startColumn = when (lastNonWordCharIndex) {
                -1 -> 0

                else -> lastNonWordCharIndex + 1
            }

            val startPosition = TextPosition(line = position.line, column = startColumn)

            val text = suggestion.text

            val existingText = when {
                startColumn < position.column -> snapshot.getTextInRange(
                    range = TextRange(start = startPosition, end = position)
                )

                else -> ""
            }

            if (existingText == text) {
                return@withLock
            }

            val operationData = when {
                startColumn < position.column -> TextOperation.Data.Batch(
                    operations = listOf(
                        TextOperation.Data.Single.Delete(range = TextRange(start = startPosition, end = position)),
                        TextOperation.Data.Single.Insert(position = startPosition, text = text)
                    )
                )

                else -> TextOperation.Data.Single.Insert(position = position, text = text)
            }

            textService.execute(
                operation = TextOperation.User(revision = snapshot.revision, data = operationData)
            ).bind()

            val updatedSnapshot = textService.snapshot.value ?: raise(IllegalStateException("Snapshot is null"))

            val targetColumn = startColumn + text.length

            val targetPosition = TextPosition(line = position.line, column = targetColumn)

            editorService.moveCaret(snapshot = updatedSnapshot, position = targetPosition).bind()

            editorService.clearSelection().bind()
        }
    }
}