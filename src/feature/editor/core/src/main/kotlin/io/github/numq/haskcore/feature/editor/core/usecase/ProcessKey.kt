package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.flatMap
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.clipboard.ClipboardService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.keymap.KeyStroke
import io.github.numq.haskcore.service.keymap.KeymapContext
import io.github.numq.haskcore.service.keymap.KeymapService
import io.github.numq.haskcore.service.text.TextService

class ProcessKey(
    private val path: String,
    private val editorService: EditorService,
    private val clipboardService: ClipboardService,
    private val documentService: DocumentService,
    private val journalService: JournalService,
    private val keymapService: KeymapService,
    private val textService: TextService
) : UseCase<ProcessKey.Input, Unit> {
    data class Input(val keyCode: Int, val modifiers: Int, val utf16CodePoint: Int)

    private fun calculateBackspaceRange(snapshot: TextSnapshot, position: TextPosition) = when (position) {
        TextPosition.ZERO -> TextRange.EMPTY

        else -> {
            val startPosition = when {
                position.column > 0 -> position.copy(column = position.column - 1)

                position.line > 0 -> {
                    val prevLine = position.line - 1

                    val prevLineLen = snapshot.getLineLength(line = prevLine)

                    TextPosition(line = prevLine, column = prevLineLen)
                }

                else -> position
            }

            TextRange(start = startPosition, end = position)
        }
    }

    private suspend fun executeUserOperation(snapshot: TextSnapshot, data: TextOperation.Data) = textService.execute(
        operation = TextOperation.User(revision = snapshot.revision, data = data)
    )

    private fun TextPosition.coerceIn(snapshot: TextSnapshot): TextPosition {
        val line = line.coerceIn(0, snapshot.lines - 1)

        val maxColumn = snapshot.getLineLength(line = line)

        return TextPosition(line = line, column = column.coerceIn(0, maxColumn))
    }

    private fun TextRange.coerceIn(snapshot: TextSnapshot) = TextRange(
        start = start.coerceIn(snapshot), end = end.coerceIn(snapshot)
    )

    private fun TextEdit.Data.toOperationData(snapshot: TextSnapshot): TextOperation.Data = when (this) {
        is TextEdit.Data.Single.Insert -> TextOperation.Data.Single.Insert(
            position = startPosition.coerceIn(snapshot), text = insertedText
        )

        is TextEdit.Data.Single.Delete -> TextOperation.Data.Single.Delete(
            range = TextRange(start = startPosition, end = oldEndPosition).coerceIn(snapshot)
        )

        is TextEdit.Data.Single.Replace -> TextOperation.Data.Single.Replace(
            range = TextRange(start = startPosition, end = oldEndPosition).coerceIn(snapshot), text = newText
        )

        is TextEdit.Data.Batch -> TextOperation.Data.Batch(operations = singles.mapNotNull { single ->
            single.toOperationData(snapshot = snapshot) as? TextOperation.Data.Single
        })
    }

    private fun TextEdit.toSystemOperation(snapshot: TextSnapshot) =
        TextOperation.System(revision = revision, data = data.toOperationData(snapshot = snapshot))

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        val snapshot = textService.snapshot.value ?: return

        val caret = editorService.caret.value

        val selection = editorService.selection.value

        if (!snapshot.isValidPosition(position = caret.position)) {
            return
        }

        val actionId = keymapService.getActionId(
            keyStroke = KeyStroke(keyCode = keyCode, modifiers = modifiers), context = KeymapContext.EDITOR
        ).bind()

        val isPrintable = Character.UnicodeBlock.of(utf16CodePoint.toChar()).let { block ->
            (!Character.isISOControl(utf16CodePoint)) && utf16CodePoint != 0xFFFF && block != null && block != Character.UnicodeBlock.SPECIALS
        }

        when {
            actionId != null -> when (actionId) {
                "editor.action.moveLeft" -> editorService.moveCaretLeft(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                "editor.action.moveRight" -> editorService.moveCaretRight(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                "editor.action.moveUp" -> editorService.moveCaretUp(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                "editor.action.moveDown" -> editorService.moveCaretDown(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                "editor.action.selectAll" -> editorService.selectAll(snapshot = snapshot).bind()

                "editor.action.save" -> documentService.saveDocument(path = path, content = snapshot.text).bind()

                "editor.action.backspace" -> when {
                    selection.range.isNotEmpty -> textService.execute(
                        operation = TextOperation.User(
                            revision = snapshot.revision,
                            data = TextOperation.Data.Single.Delete(range = selection.range)
                        )
                    ).bind()

                    caret.position != TextPosition.ZERO -> {
                        val rangeToDelete = calculateBackspaceRange(snapshot = snapshot, position = caret.position)

                        val data = TextOperation.Data.Single.Delete(range = rangeToDelete)

                        executeUserOperation(snapshot = snapshot, data = data).bind()
                    }
                }

                "editor.action.enter" -> {
                    val data = TextOperation.Data.Single.Insert(position = caret.position, text = "\n")

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }

                "editor.action.tab" -> {
                    val tabText = "    "

                    val data = TextOperation.Data.Single.Insert(position = caret.position, text = tabText)

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }

                "editor.action.undo" -> journalService.undo(revision = snapshot.revision).bind()?.let { edit ->
                    textService.execute(operation = edit.toSystemOperation(snapshot = snapshot)).bind()
                }

                "editor.action.redo" -> journalService.redo(revision = snapshot.revision).bind()?.let { edit ->
                    textService.execute(operation = edit.toSystemOperation(snapshot = snapshot)).bind()
                }

                "editor.action.cut" -> {
                    val range = selection.range.takeIf(TextRange::isNotEmpty) ?: return

                    val text = snapshot.getTextInRange(range = range)

                    val data = TextOperation.Data.Single.Delete(range = range)

                    clipboardService.copyToClipboard(text = text).flatMap {
                        executeUserOperation(snapshot = snapshot, data = data)
                    }
                }

                "editor.action.copy" -> {
                    val range = selection.range.takeIf(TextRange::isNotEmpty) ?: return

                    val text = snapshot.getTextInRange(range = range)

                    clipboardService.copyToClipboard(text = text).bind()
                }

                "editor.action.paste" -> {
                    val text = clipboardService.clipboard.value.text?.takeIf(String::isNotEmpty) ?: return

                    val data = when {
                        selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                            range = selection.range, text = text
                        )

                        else -> TextOperation.Data.Single.Insert(position = caret.position, text = text)
                    }

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }
            }

            isPrintable -> {
                val text = String(Character.toChars(utf16CodePoint))

                val data = when {
                    selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                        range = selection.range, text = text
                    )

                    else -> TextOperation.Data.Single.Insert(position = caret.position, text = text)
                }

                executeUserOperation(snapshot = snapshot, data = data).bind()
            }

            else -> Unit
        }
    }
}