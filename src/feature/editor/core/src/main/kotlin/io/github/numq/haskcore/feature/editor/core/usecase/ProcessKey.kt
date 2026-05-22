package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.Raise
import arrow.core.right
import io.github.numq.haskcore.common.core.text.*
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.service.clipboard.ClipboardService
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.keymap.KeymapAction
import io.github.numq.haskcore.service.keymap.KeymapContext
import io.github.numq.haskcore.service.keymap.KeymapService
import io.github.numq.haskcore.service.text.TextService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProcessKey(
    private val path: String,
    private val editorService: EditorService,
    private val clipboardService: ClipboardService,
    private val documentService: DocumentService,
    private val journalService: JournalService,
    private val keymapService: KeymapService,
    private val textService: TextService,
) : UseCase.Command<ProcessKey.Input> {
    data class Input(val keyCode: Int, val modifiers: Int, val utf16CodePoint: Int)

    private val mutex = Mutex()

    private fun calculateBackspaceRange(snapshot: TextSnapshot, position: TextPosition) = when (position) {
        TextPosition.ZERO -> TextRange.EMPTY

        else -> {
            val startPosition = when {
                position.column > 0 -> position.copy(column = position.column - 1)

                position.line > 0 -> {
                    val previousLine = position.line - 1

                    TextPosition(line = previousLine, column = snapshot.getLineLength(line = previousLine))
                }

                else -> position
            }

            TextRange(start = startPosition, end = position)
        }
    }

    private fun calculateDeleteRange(snapshot: TextSnapshot, position: TextPosition): TextRange {
        val lineLength = snapshot.getLineLength(line = position.line)

        return when {
            position.column < lineLength -> {
                TextRange(start = position, end = position.copy(column = position.column + 1))
            }

            position.line < snapshot.lines - 1 -> {
                TextRange(start = position, end = TextPosition(line = position.line + 1, column = 0))
            }

            else -> TextRange.EMPTY
        }
    }

    private suspend fun getWordBoundaryLeft(snapshot: TextSnapshot, position: TextPosition): TextPosition {
        if (position.column == 0) {
            when {
                position.line > 0 -> {
                    val previousLine = position.line - 1

                    TextPosition(line = previousLine, column = snapshot.getLineLength(line = previousLine))
                }

                else -> position
            }
        }

        val lineText = snapshot.getTextInRange(TextRange(TextPosition(position.line, 0), position))

        var column = position.column - 1

        while (currentCoroutineContext().isActive && column > 0 && lineText[column].isWhitespace()) {
            column--
        }

        when {
            column >= 0 && lineText[column].isLetterOrDigit() -> while (currentCoroutineContext().isActive && column > 0 && lineText[column - 1].isLetterOrDigit()) {
                column--
            }

            column > 0 -> column--
        }

        return TextPosition(line = position.line, column = column)
    }

    private suspend fun getWordBoundaryRight(snapshot: TextSnapshot, position: TextPosition): TextPosition {
        val lineLength = snapshot.getLineLength(line = position.line)

        if (position.column >= lineLength) {
            when {
                position.line < snapshot.lines - 1 -> TextPosition(line = position.line + 1, column = 0)

                else -> position
            }
        }

        val lineText = snapshot.getTextInRange(
            range = TextRange(
                start = position, end = TextPosition(line = position.line, column = lineLength)
            )
        )

        var column = 0

        val max = lineText.length

        when {
            max == 0 -> position

            lineText[column].isLetterOrDigit() -> while (currentCoroutineContext().isActive && column < max && lineText[column].isLetterOrDigit()) {
                column++
            }

            lineText[column].isWhitespace() -> {
                while (currentCoroutineContext().isActive && column < max && lineText[column].isWhitespace()) {
                    column++
                }

                when {
                    column < max && lineText[column].isLetterOrDigit() -> while (currentCoroutineContext().isActive && column < max && lineText[column].isLetterOrDigit()) {
                        column++
                    }

                    column < max -> column++
                }
            }

            else -> column++
        }

        return TextPosition(line = position.line, column = position.column + column)
    }

    private suspend fun handleMovement(
        snapshot: TextSnapshot,
        withSelection: Boolean,
        targetPositionProvider: suspend () -> TextPosition,
    ): Either<Throwable, Unit> {
        val oldCaret = editorService.caret.value.position

        val targetPosition = targetPositionProvider()

        return when {
            withSelection -> {
                val selectionInit = when {
                    editorService.selection.value.range.isEmpty -> editorService.startSelection(
                        snapshot = snapshot, position = oldCaret
                    )

                    else -> Unit.right()
                }

                selectionInit.flatMap {
                    editorService.moveCaret(snapshot = snapshot, position = targetPosition)
                }.flatMap {
                    editorService.extendSelection(snapshot = snapshot, position = targetPosition)
                }
            }

            else -> editorService.moveCaret(snapshot = snapshot, position = targetPosition).flatMap {
                editorService.clearSelection()
            }
        }
    }

    private suspend fun executeUserOperation(snapshot: TextSnapshot, data: TextOperation.Data) = textService.execute(
        operation = TextOperation.User(revision = snapshot.revision, data = data)
    )

    private suspend fun deleteRangeOrSelection(
        snapshot: TextSnapshot,
        selection: Selection,
        caret: Caret,
        rangeProvider: suspend (TextSnapshot, TextPosition) -> TextRange,
    ) = when {
        selection.range.isNotEmpty -> executeUserOperation(
            snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = selection.range)
        )

        else -> {
            val range = rangeProvider(snapshot, caret.position)

            when {
                range.isNotEmpty -> executeUserOperation(
                    snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = range)
                )

                else -> Unit.right()
            }
        }
    }

    private fun TextPosition.coerceIn(snapshot: TextSnapshot) = when (snapshot.lines) {
        0 -> TextPosition.ZERO

        else -> {
            val line = line.coerceIn(0, snapshot.lines - 1)

            TextPosition(line = line, column = column.coerceIn(0, snapshot.getLineLength(line = line)))
        }
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

    private fun TextEdit.toSystemOperation(snapshot: TextSnapshot) = TextOperation.System(
        revision = snapshot.revision, data = data.toOperationData(snapshot = snapshot)
    )

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        mutex.withLock {
            val snapshot = textService.snapshot.value ?: return@withLock

            val caret = editorService.caret.value

            val selection = editorService.selection.value

            if (!snapshot.isValidPosition(position = caret.position)) return@withLock

            val action = keymapService.findAction(
                keyCode = keyCode, modifiers = modifiers, context = KeymapContext.EDITOR
            ).bind()

            val isPrintable = Character.UnicodeBlock.of(utf16CodePoint.toChar()).let { block ->
                (!Character.isISOControl(utf16CodePoint)) && utf16CodePoint != 0xFFFF && block != null && block != Character.UnicodeBlock.SPECIALS
            }

            when (action) {
                KeymapAction.Navigation.Move.Left -> editorService.moveCaretLeft(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                KeymapAction.Navigation.Move.Right -> editorService.moveCaretRight(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                KeymapAction.Navigation.Move.Up -> editorService.moveCaretUp(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                KeymapAction.Navigation.Move.Down -> editorService.moveCaretDown(snapshot = snapshot).flatMap {
                    editorService.clearSelection()
                }.bind()

                KeymapAction.Navigation.Move.LeftWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    val current = editorService.caret.value.position

                    when {
                        current.column > 0 -> current.copy(column = current.column - 1)

                        current.line > 0 -> TextPosition(
                            line = current.line - 1, column = snapshot.getLineLength(line = current.line - 1)
                        )

                        else -> current
                    }
                }.bind()

                KeymapAction.Navigation.Move.RightWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    val current = editorService.caret.value.position

                    val maxColumn = snapshot.getLineLength(line = current.line)

                    when {
                        current.column < maxColumn -> current.copy(column = current.column + 1)

                        current.line < snapshot.lines - 1 -> TextPosition(line = current.line + 1, column = 0)

                        else -> current
                    }
                }.bind()

                KeymapAction.Navigation.Move.UpWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    val current = editorService.caret.value.position

                    when {
                        current.line > 0 -> {
                            val previousLine = current.line - 1

                            TextPosition(
                                line = previousLine,
                                column = minOf(current.column, snapshot.getLineLength(line = previousLine))
                            )
                        }

                        else -> current
                    }
                }.bind()

                KeymapAction.Navigation.Move.DownWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    val current = editorService.caret.value.position

                    when {
                        current.line < snapshot.lines - 1 -> {
                            val nextLine = current.line + 1

                            TextPosition(
                                line = nextLine, column = minOf(current.column, snapshot.getLineLength(line = nextLine))
                            )
                        }

                        else -> current
                    }
                }.bind()

                KeymapAction.Navigation.WordMove.Left -> handleMovement(snapshot = snapshot, withSelection = false) {
                    getWordBoundaryLeft(snapshot = snapshot, position = editorService.caret.value.position)
                }.bind()

                KeymapAction.Navigation.WordMove.Right -> handleMovement(snapshot = snapshot, withSelection = false) {
                    getWordBoundaryRight(snapshot = snapshot, position = editorService.caret.value.position)
                }.bind()

                KeymapAction.Navigation.WordMove.LeftWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    getWordBoundaryLeft(snapshot = snapshot, position = editorService.caret.value.position)
                }.bind()

                KeymapAction.Navigation.WordMove.RightWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    getWordBoundaryRight(snapshot = snapshot, editorService.caret.value.position)
                }.bind()

                KeymapAction.Navigation.LineMove.Start -> handleMovement(snapshot = snapshot, withSelection = false) {
                    TextPosition(line = caret.position.line, column = 0)
                }.bind()

                KeymapAction.Navigation.LineMove.End -> handleMovement(snapshot = snapshot, withSelection = false) {
                    TextPosition(
                        line = caret.position.line, column = snapshot.getLineLength(line = caret.position.line)
                    )
                }.bind()

                KeymapAction.Navigation.LineMove.StartWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    TextPosition(line = caret.position.line, column = 0)
                }.bind()

                KeymapAction.Navigation.LineMove.EndWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    TextPosition(
                        line = caret.position.line, column = snapshot.getLineLength(line = caret.position.line)
                    )
                }.bind()

                KeymapAction.Navigation.DocumentMove.Start -> handleMovement(
                    snapshot = snapshot, withSelection = false
                ) {
                    TextPosition.ZERO
                }.bind()

                KeymapAction.Navigation.DocumentMove.End -> handleMovement(snapshot = snapshot, withSelection = false) {
                    val lastLine = snapshot.lines - 1

                    TextPosition(line = lastLine, column = snapshot.getLineLength(line = lastLine))
                }.bind()

                KeymapAction.Navigation.DocumentMove.StartWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    TextPosition.ZERO
                }.bind()

                KeymapAction.Navigation.DocumentMove.EndWithSelection -> handleMovement(
                    snapshot = snapshot, withSelection = true
                ) {
                    val lastLine = snapshot.lines - 1

                    TextPosition(line = lastLine, column = snapshot.getLineLength(line = lastLine))
                }.bind()

                KeymapAction.Editing.Basic.Backspace -> deleteRangeOrSelection(
                    snapshot, selection, caret
                ) { snapshot, position ->
                    calculateBackspaceRange(snapshot = snapshot, position = position)
                }.bind()

                KeymapAction.Editing.Basic.Delete -> deleteRangeOrSelection(
                    snapshot, selection, caret
                ) { snapshot, position ->
                    calculateDeleteRange(snapshot = snapshot, position = position)
                }.bind()

                KeymapAction.Editing.Basic.Enter -> {
                    val data = when {
                        selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                            range = selection.range, text = "\n"
                        )

                        else -> TextOperation.Data.Single.Insert(position = caret.position, text = "\n")
                    }

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }

                KeymapAction.Editing.Basic.Tab -> {
                    val tabText = "    "

                    val data = when {
                        selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                            range = selection.range, text = tabText
                        )

                        else -> TextOperation.Data.Single.Insert(position = caret.position, text = tabText)
                    }

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }

                KeymapAction.Editing.WordDelete.Left -> when {
                    selection.range.isNotEmpty -> executeUserOperation(
                        snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = selection.range)
                    ).bind()

                    else -> {
                        val targetPosition = getWordBoundaryLeft(snapshot = snapshot, position = caret.position)

                        if (targetPosition != caret.position) {
                            executeUserOperation(
                                snapshot = snapshot, data = TextOperation.Data.Single.Delete(
                                    range = TextRange(
                                        start = targetPosition, end = caret.position
                                    )
                                )
                            ).bind()
                        }
                    }
                }

                KeymapAction.Editing.WordDelete.Right -> when {
                    selection.range.isNotEmpty -> executeUserOperation(
                        snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = selection.range)
                    ).bind()

                    else -> {
                        val targetPosition = getWordBoundaryRight(snapshot = snapshot, position = caret.position)

                        if (targetPosition != caret.position) {
                            executeUserOperation(
                                snapshot = snapshot, data = TextOperation.Data.Single.Delete(
                                    range = TextRange(
                                        start = caret.position, end = targetPosition
                                    )
                                )
                            ).bind()
                        }
                    }
                }

                KeymapAction.Editing.LineOperation.Duplicate -> {
                    val currentLine = caret.position.line

                    val currentColumn = caret.position.column

                    val lineLength = snapshot.getLineLength(currentLine)

                    val lineText = snapshot.getTextInRange(
                        range = TextRange(
                            start = TextPosition(line = currentLine, column = 0),
                            end = TextPosition(line = currentLine, column = lineLength)
                        )
                    )

                    executeUserOperation(
                        snapshot, TextOperation.Data.Single.Insert(
                            position = TextPosition(line = currentLine + 1, column = 0), text = "$lineText\n"
                        )
                    ).flatMap {
                        editorService.moveCaret(
                            snapshot = textService.snapshot.value ?: return@flatMap Either.Left(
                                IllegalStateException("Snapshot is null")
                            ), position = TextPosition(
                                line = currentLine + 1, column = minOf(currentColumn, lineLength)
                            )
                        )
                    }.flatMap {
                        editorService.clearSelection()
                    }.bind()
                }

                KeymapAction.Editing.LineOperation.Delete -> {
                    val currentLine = caret.position.line

                    val currentColumn = caret.position.column

                    val startPosition = TextPosition(line = currentLine, column = 0)

                    val endPosition = when {
                        currentLine < snapshot.lines - 1 -> TextPosition(line = currentLine + 1, column = 0)

                        else -> TextPosition(line = currentLine, column = snapshot.getLineLength(currentLine))
                    }

                    val range = TextRange(startPosition, endPosition)

                    if (range.isNotEmpty) {
                        executeUserOperation(
                            snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = range)
                        ).flatMap {
                            val updatedSnapshot = textService.snapshot.value ?: return@flatMap Either.Left(
                                IllegalStateException("Snapshot is null")
                            )

                            val targetLine = when {
                                currentLine < updatedSnapshot.lines -> currentLine

                                else -> (currentLine - 1).coerceAtLeast(0)
                            }

                            val targetLineLength = updatedSnapshot.getLineLength(line = targetLine)

                            editorService.moveCaret(
                                snapshot = updatedSnapshot, position = TextPosition(
                                    line = targetLine, column = minOf(currentColumn, targetLineLength)
                                )
                            )
                        }.flatMap {
                            editorService.clearSelection()
                        }.bind()
                    }
                }

                KeymapAction.Clipboard.Cut -> {
                    val range = selection.range.takeIf(TextRange::isNotEmpty) ?: run {
                        val currentLine = caret.position.line

                        val lineLength = snapshot.getLineLength(currentLine)

                        val endPosition = when {
                            currentLine < snapshot.lines - 1 -> TextPosition(line = currentLine + 1, column = 0)

                            else -> TextPosition(line = currentLine, column = lineLength)
                        }

                        TextRange(start = TextPosition(line = currentLine, column = 0), end = endPosition)
                    }

                    val text = snapshot.getTextInRange(range = range)

                    clipboardService.copyToClipboard(text = text).flatMap {
                        executeUserOperation(
                            snapshot = snapshot, data = TextOperation.Data.Single.Delete(range = range)
                        )
                    }.bind()
                }

                KeymapAction.Clipboard.Copy -> {
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

                KeymapAction.Clipboard.Paste -> {
                    val text = clipboardService.clipboard.value.text?.takeIf(String::isNotEmpty) ?: return@withLock

                    val data = when {
                        selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                            range = selection.range, text = text
                        )

                        else -> TextOperation.Data.Single.Insert(
                            position = caret.position, text = text
                        )
                    }

                    executeUserOperation(snapshot = snapshot, data = data).bind()
                }

                KeymapAction.History.Undo -> journalService.undo(revision = snapshot.revision).bind()?.let { edit ->
                    textService.execute(operation = edit.toSystemOperation(snapshot = snapshot)).bind()
                }

                KeymapAction.History.Redo, KeymapAction.History.RedoAlt -> journalService.redo(
                    revision = snapshot.revision
                ).bind()?.let { edit ->
                    textService.execute(operation = edit.toSystemOperation(snapshot = snapshot)).bind()
                }

                KeymapAction.File.SelectAll -> editorService.selectAll(snapshot = snapshot).bind()

                KeymapAction.File.Save -> documentService.saveDocument(
                    path = path, content = snapshot.text
                ).bind()

                null -> {
                    if (isPrintable) {
                        val text = String(Character.toChars(utf16CodePoint))

                        val data = when {
                            selection.range.isNotEmpty -> TextOperation.Data.Single.Replace(
                                range = selection.range, text = text
                            )

                            else -> TextOperation.Data.Single.Insert(
                                position = caret.position, text = text
                            )
                        }

                        executeUserOperation(snapshot = snapshot, data = data).bind()
                    }
                }
            }
        }
    }
}