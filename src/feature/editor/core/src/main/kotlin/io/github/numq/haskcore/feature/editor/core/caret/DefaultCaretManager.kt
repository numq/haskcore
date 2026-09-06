package io.github.numq.haskcore.feature.editor.core.caret

import arrow.core.Either
import arrow.core.raise.either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextSnapshot
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultCaretManager(private val scope: CoroutineScope) : CaretManager {
    private val _stickyColumn = atomic(0)

    private val _caret = MutableStateFlow(Caret.ZERO)

    override val caret = _caret.asStateFlow()

    private fun transformSingle(current: TextPosition, data: TextEdit.Data.Single): TextPosition {
        if (current < data.startPosition) return current

        val oldEnd = when (data) {
            is TextEdit.Data.Single.Insert -> data.startPosition

            is TextEdit.Data.Single.Delete -> data.oldEndPosition

            is TextEdit.Data.Single.Replace -> data.oldEndPosition
        }

        if (current <= oldEnd) {
            return when (data) {
                is TextEdit.Data.Single.Insert -> data.newEndPosition

                is TextEdit.Data.Single.Delete -> data.startPosition

                is TextEdit.Data.Single.Replace -> data.newEndPosition
            }
        }

        val lineDiff = data.newEndPosition.line - oldEnd.line

        return when (current.line) {
            oldEnd.line -> {
                val columnDiff = data.newEndPosition.column - oldEnd.column

                TextPosition(line = current.line + lineDiff, column = current.column + columnDiff)
            }

            else -> TextPosition(line = current.line + lineDiff, column = current.column)
        }
    }

    private fun transformPosition(current: TextPosition, data: TextEdit.Data) = when (data) {
        is TextEdit.Data.Single -> transformSingle(current = current, data = data)

        is TextEdit.Data.Batch -> data.singles.fold(current) { position, single ->
            transformSingle(current = position, data = single)
        }
    }

    private fun updateCaretOnly(snapshot: TextSnapshot, position: TextPosition): Caret? {
        val totalLines = snapshot.lines

        return when {
            totalLines > 0 -> {

                val line = position.line.coerceIn(0, totalLines - 1)

                val lineLength = snapshot.getLineLength(line = line)

                val column = position.column.coerceIn(0, lineLength)

                val caret = Caret(position = TextPosition(line = line, column = column))

                _caret.value = caret

                caret
            }

            else -> null
        }
    }

    override suspend fun handleTextEdit(snapshot: TextSnapshot, data: TextEdit.Data): Either<Throwable, Unit> = either {
        val currentPosition = _caret.value.position

        val nextPosition = transformPosition(current = currentPosition, data = data)

        val validPosition = nextPosition.coerceIn(snapshot = snapshot)

        _caret.value = Caret(position = validPosition)

        _stickyColumn.value = validPosition.column
    }

    override suspend fun moveTo(snapshot: TextSnapshot, position: TextPosition): Either<Throwable, Unit> = either {
        val validPosition = position.coerceIn(snapshot = snapshot)

        updateCaretOnly(snapshot = snapshot, position = validPosition)

        _stickyColumn.value = validPosition.column
    }

    override suspend fun moveLeft(
        snapshot: TextSnapshot,
        collapsedRanges: List<IntRange>,
    ): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        var next = when {
            current.column > 0 -> current.copy(column = current.column - 1)

            current.line > 0 -> {
                var prevLine = current.line - 1

                while (prevLine >= 0 && collapsedRanges.any { it.contains(prevLine) }) {
                    prevLine--
                }

                if (prevLine >= 0) {
                    val prevLineLen = snapshot.getLineLength(line = prevLine)
                    TextPosition(line = prevLine, column = prevLineLen)
                } else current
            }

            else -> current
        }

        if (next != current) {
            val validPosition = next.coerceIn(snapshot = snapshot)

            updateCaretOnly(snapshot = snapshot, position = validPosition)

            _stickyColumn.value = next.column
        }
    }

    override suspend fun moveRight(
        snapshot: TextSnapshot,
        collapsedRanges: List<IntRange>,
    ): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        val currentLineLength = snapshot.getLineLength(line = current.line)

        var next = when {
            current.column < currentLineLength -> current.copy(column = current.column + 1)

            current.line < snapshot.lines - 1 -> {
                var nextLine = current.line + 1

                while (nextLine < snapshot.lines && collapsedRanges.any { it.contains(nextLine) }) {
                    nextLine++
                }

                if (nextLine < snapshot.lines) {
                    TextPosition(line = nextLine, column = 0)
                } else current
            }

            else -> current
        }

        if (next != current) {
            val validPosition = next.coerceIn(snapshot = snapshot)

            updateCaretOnly(snapshot = snapshot, position = validPosition)

            _stickyColumn.value = next.column
        }
    }

    override suspend fun moveUp(
        snapshot: TextSnapshot,
        collapsedRanges: List<IntRange>,
    ): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        var nextLine = current.line - 1

        while (nextLine >= 0 && collapsedRanges.any { it.contains(nextLine) }) {
            nextLine--
        }

        if (nextLine >= 0) {
            val nextLineLength = snapshot.getLineLength(line = nextLine)

            val targetColumn = _stickyColumn.value.coerceAtMost(nextLineLength)

            val next = TextPosition(line = nextLine, column = targetColumn)

            if (next != current) {
                updateCaretOnly(snapshot = snapshot, position = next)
            }
        }
    }

    override suspend fun moveDown(
        snapshot: TextSnapshot,
        collapsedRanges: List<IntRange>,
    ): Either<Throwable, Unit> = either {
        val current = _caret.value.position

        var nextLine = current.line + 1

        while (nextLine < snapshot.lines && collapsedRanges.any { it.contains(nextLine) }) {
            nextLine++
        }

        if (nextLine < snapshot.lines) {
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