package io.github.numq.haskcore.feature.editor.core.caret

import arrow.core.Either
import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultCaretManager(private val scope: CoroutineScope) : CaretManager {
    private val _stickyColumn = atomic(0)

    private val _caret = MutableStateFlow(Caret.ZERO)

    override val caret = _caret.asStateFlow()

    private fun transformSingle(current: TextPosition, data: TextEdit.Data.Single) = when {
        current < data.startPosition -> current

        current <= data.oldEndPosition -> data.newEndPosition

        else -> {
            val lineDiff = data.newEndPosition.line - data.oldEndPosition.line

            when (current.line) {
                data.oldEndPosition.line -> {
                    val columnDiff = data.newEndPosition.column - data.oldEndPosition.column

                    TextPosition(line = current.line + lineDiff, column = current.column + columnDiff)
                }

                else -> current.copy(line = current.line + lineDiff)
            }
        }
    }

    private fun transformPosition(current: TextPosition, data: TextEdit.Data) = when (data) {
        is TextEdit.Data.Single -> transformSingle(current = current, data = data)

        is TextEdit.Data.Batch -> data.singles.fold(current) { position, single ->
            transformSingle(current = position, data = single)
        }
    }

    private fun updateCaretOnly(snapshot: TextSnapshot, position: TextPosition) {
        val totalLines = snapshot.lines

        if (totalLines > 0) {
            val line = position.line.coerceIn(0, totalLines - 1)

            val lineLength = snapshot.getLineLength(line = line)

            val column = position.column.coerceIn(0, lineLength)

            _caret.value = Caret(position = TextPosition(line = line, column = column))
        }
    }

    private fun TextPosition.coerceIn(snapshot: TextSnapshot): TextPosition {
        val line = line.coerceIn(0, snapshot.lines - 1)

        val maxColumn = snapshot.getLineLength(line = line)

        return TextPosition(line = line, column = column.coerceIn(0, maxColumn))
    }

    override suspend fun handleTextEdit(snapshot: TextSnapshot, data: TextEdit.Data): Either<Throwable, Unit> = either {
        val currentPosition = _caret.value.position

        val nextPosition = transformPosition(current = currentPosition, data = data)

        val validPosition = nextPosition.coerceIn(snapshot)

        _caret.value = Caret(position = validPosition)

        _stickyColumn.value = validPosition.column
    }

    override suspend fun moveTo(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit> = either {
        val validPosition = position.coerceIn(snapshot)

        updateCaretOnly(snapshot = snapshot, position = validPosition)

        _stickyColumn.value = validPosition.column
    }

    override suspend fun moveLeft(snapshot: TextSnapshot): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        val next = when {
            current.column > 0 -> current.copy(column = current.column - 1)

            current.line > 0 -> {
                val prevLine = current.line - 1

                val prevLineLen = snapshot.getLineLength(line = prevLine)

                TextPosition(line = prevLine, column = prevLineLen)
            }

            else -> current
        }

        if (next != current) {
            val validPosition = next.coerceIn(snapshot)

            updateCaretOnly(snapshot = snapshot, position = validPosition)

            _stickyColumn.value = next.column
        }
    }

    override suspend fun moveRight(snapshot: TextSnapshot): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        val currentLineLength = snapshot.getLineLength(line = current.line)

        val next = when {
            current.column < currentLineLength -> current.copy(column = current.column + 1)

            current.line < snapshot.lines - 1 -> TextPosition(line = current.line + 1, column = 0)

            else -> current
        }

        if (next != current) {
            val validPosition = next.coerceIn(snapshot)

            updateCaretOnly(snapshot = snapshot, position = validPosition)

            _stickyColumn.value = next.column
        }
    }

    override suspend fun moveUp(snapshot: TextSnapshot): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        if (current.line > 0) {
            val nextLine = current.line - 1

            val nextLineLength = snapshot.getLineLength(line = nextLine)

            val targetColumn = _stickyColumn.value.coerceAtMost(nextLineLength)

            val next = TextPosition(line = nextLine, column = targetColumn)

            if (next != current) {
                updateCaretOnly(snapshot = snapshot, position = next)
            }
        }
    }

    override suspend fun moveDown(snapshot: TextSnapshot): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        if (current.line < snapshot.lines - 1) {
            val nextLine = current.line + 1

            val nextLineLength = snapshot.getLineLength(line = nextLine)

            val targetColumn = _stickyColumn.value.coerceAtMost(nextLineLength)

            val next = TextPosition(line = nextLine, column = targetColumn)

            if (next != current) {
                updateCaretOnly(snapshot = snapshot, position = next)
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}