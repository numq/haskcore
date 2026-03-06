package io.github.numq.haskcore.service.text.buffer

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.github.numq.haskcore.core.text.LineEnding
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.buffer.rope.Rope
import io.github.numq.haskcore.service.text.buffer.rope.RopeNavigator
import io.github.numq.haskcore.service.text.buffer.rope.RopeNodeLeafFactory
import io.github.numq.haskcore.service.text.snapshot.ImmutableTextSnapshot
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal class RopeTextBuffer(
    private val initialText: String,
    initialLineEnding: LineEnding = LineEnding.analyze(initialText).dominant,
    initialCharset: Charset = StandardCharsets.UTF_8
) : TextBuffer {
    private companion object {
        const val DATA_CAPACITY = 64
    }

    private val mutex = Mutex()

    private val currentRevision = atomic(0L)

    private val _rope = MutableStateFlow(initializeRope(text = initialText, charset = initialCharset))

    private val _lineEnding = MutableStateFlow(initialLineEnding)

    private val _charset = MutableStateFlow(initialCharset)

    private val _data = MutableSharedFlow<TextEdit.Data>(replay = 0, extraBufferCapacity = DATA_CAPACITY)

    override val data = _data.asSharedFlow()

    private val _snapshot = MutableStateFlow(
        ImmutableTextSnapshot(
            rope = _rope.value,
            revision = currentRevision.value,
            charset = _charset.value,
            lineEnding = _lineEnding.value
        )
    )

    override val snapshot = _snapshot.asStateFlow()

    private fun normalizeLineEndings(text: String) = when {
        !text.contains('\r') -> text

        else -> text.replace("\r\n", "\n").replace("\r", "\n")
    }

    private fun initializeRope(text: String, charset: Charset): Rope {
        val normalizedText = when {
            text.isEmpty() -> text

            else -> normalizeLineEndings(text = text)
        }

        return Rope(
            initialText = normalizedText,
            charset = charset,
            ropeNodeLeafFactory = RopeNodeLeafFactory(enablePooling = true, charset = charset)
        )
    }

    private fun doInsert(rope: Rope, position: TextPosition, text: String) = Either.catch {
        require(RopeNavigator.isValidInsertPosition(rope = rope, position = position)) { "Invalid position: $position" }

        when {
            text.isEmpty() -> null

            else -> {
                val charOffset = RopeNavigator.getCharOffset(
                    rope = rope, position = position
                ).coerceIn(0, rope.totalChars)

                val normalizedText = normalizeLineEndings(text = text)

                val startByte = rope.getByteOffset(charOffset = charOffset)

                val newRope = rope.insert(offset = charOffset, text = normalizedText)

                val newEndCharOffset = charOffset + normalizedText.length

                val newEndByte = newRope.getByteOffset(charOffset = newEndCharOffset)

                val newEndPos = RopeNavigator.getPosition(rope = newRope, charOffset = newEndCharOffset)

                newRope to TextEdit.Data.Single.Insert(
                    startPosition = position,
                    newEndPosition = newEndPos,
                    insertedText = normalizedText,
                    startByte = startByte,
                    newEndByte = newEndByte
                )
            }
        }
    }

    private fun doReplace(rope: Rope, range: TextRange, text: String) = Either.catch {
        when {
            range.isEmpty && text.isEmpty() -> null

            else -> {
                val normalizedText = normalizeLineEndings(text = text)

                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                val newRope = when {
                    range.isEmpty -> rope.insert(offset = startOffset, text = normalizedText)

                    text.isEmpty() -> rope.delete(offset = startOffset, length = endOffset - startOffset)

                    else -> rope.delete(offset = startOffset, length = endOffset - startOffset).insert(
                        offset = startOffset, text = normalizedText
                    )
                }

                val startByte = rope.getByteOffset(charOffset = startOffset)

                val oldEndByte = rope.getByteOffset(charOffset = endOffset)

                val oldText = when {
                    range.isEmpty -> ""

                    else -> rope.getText(offset = startOffset, length = endOffset - startOffset)
                }

                val newEndCharOffset = startOffset + normalizedText.length

                val newEndByte = newRope.getByteOffset(charOffset = newEndCharOffset)

                val newEndPos = RopeNavigator.getPosition(rope = newRope, charOffset = newEndCharOffset)

                newRope to when {
                    range.isEmpty -> TextEdit.Data.Single.Insert(
                        startPosition = range.start,
                        newEndPosition = newEndPos,
                        insertedText = normalizedText,
                        startByte = startByte,
                        newEndByte = newEndByte
                    )

                    text.isEmpty() -> TextEdit.Data.Single.Delete(
                        startPosition = range.start,
                        oldEndPosition = range.end,
                        deletedText = oldText,
                        startByte = startByte,
                        oldEndByte = oldEndByte
                    )

                    else -> TextEdit.Data.Single.Replace(
                        startPosition = range.start,
                        oldEndPosition = range.end,
                        newEndPosition = newEndPos,
                        oldText = oldText,
                        newText = normalizedText,
                        startByte = startByte,
                        oldEndByte = oldEndByte,
                        newEndByte = newEndByte
                    )
                }
            }
        }
    }

    private fun doDelete(rope: Rope, range: TextRange) = Either.catch {
        when {
            range.isEmpty -> null

            else -> {
                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                val newRope = rope.delete(offset = startOffset, length = endOffset - startOffset)

                val startByte = rope.getByteOffset(charOffset = startOffset)

                val oldEndByte = rope.getByteOffset(charOffset = endOffset)

                val oldText = rope.getText(offset = startOffset, length = endOffset - startOffset)

                newRope to TextEdit.Data.Single.Delete(
                    startPosition = range.start,
                    oldEndPosition = range.end,
                    deletedText = oldText,
                    startByte = startByte,
                    oldEndByte = oldEndByte
                )
            }
        }
    }

    private suspend fun updateRope(result: Pair<Rope, TextEdit.Data>?) = result?.let { (newRope, data) ->
        val nextRevision = currentRevision.incrementAndGet()

        val snapshot = ImmutableTextSnapshot(
            rope = newRope, revision = nextRevision, charset = _charset.value, lineEnding = _lineEnding.value
        )

        _rope.value = newRope

        _snapshot.value = snapshot

        _data.emit(data)

        data
    }

    override suspend fun changeLineEnding(lineEnding: LineEnding) = Either.catch {
        _lineEnding.value = lineEnding
    }

    override suspend fun changeCharset(charset: Charset) = Either.catch {
        mutex.withLock {
            if (_charset.value != charset) {
                val newCharset = _charset.updateAndGet { charset }

                _rope.update { rope ->
                    rope.rebuildWithCharset(newCharset = newCharset)
                }
            }
        }
    }

    override suspend fun insert(position: TextPosition, text: String) = either {
        mutex.withLock {
            val rope = _rope.value

            val result = doInsert(rope = rope, position = position, text = text).bind()

            updateRope(result = result) as? TextEdit.Data.Single.Insert
        }
    }

    override suspend fun replace(range: TextRange, text: String) = either {
        mutex.withLock {
            val rope = _rope.value

            val result = doReplace(rope = rope, range = range, text = text).bind()

            updateRope(result = result) as? TextEdit.Data.Single.Replace
        }
    }

    override suspend fun delete(range: TextRange) = either {
        mutex.withLock {
            val rope = _rope.value

            val result = doDelete(rope = rope, range = range).bind()

            updateRope(result = result) as? TextEdit.Data.Single.Delete
        }
    }

    override suspend fun withBatch(block: suspend Raise<Throwable>.(TextBuffer) -> Unit) = either {
        mutex.withLock {
            var currentRope = _rope.value

            val accumulatedSingles = mutableListOf<TextEdit.Data.Single>()

            val batchBuffer: TextBuffer = object : TextBuffer by this@RopeTextBuffer {
                override val data = this@RopeTextBuffer.data

                override val snapshot = this@RopeTextBuffer.snapshot

                override suspend fun changeLineEnding(lineEnding: LineEnding) = either {
                    raise(IllegalStateException("Cannot change line endings during a batch operation"))
                }

                override suspend fun changeCharset(charset: Charset) = either {
                    raise(IllegalStateException("Cannot change charset during a batch operation"))
                }

                private suspend fun applyToBatch(
                    action: suspend (Rope) -> Either<Throwable, Pair<Rope, TextEdit.Data.Single>?>
                ): Either<Throwable, TextEdit.Data.Single> = either {
                    when (val result = action(currentRope).bind()) {
                        null -> TextEdit.Data.Single.Insert(
                            startPosition = TextPosition.ZERO,
                            newEndPosition = TextPosition.ZERO,
                            insertedText = "",
                            startByte = 0,
                            newEndByte = 0
                        )

                        else -> {
                            val (nextRope, data) = result

                            currentRope = nextRope

                            accumulatedSingles.add(data)

                            data
                        }
                    }
                }

                override suspend fun insert(position: TextPosition, text: String) = applyToBatch { rope ->
                    doInsert(rope = rope, position = position, text = text)
                }

                override suspend fun delete(range: TextRange) = applyToBatch { rope ->
                    doDelete(rope = rope, range = range)
                }

                override suspend fun replace(range: TextRange, text: String) = applyToBatch { rope ->
                    doReplace(rope = rope, range = range, text = text)
                }

                override suspend fun withBatch(block: suspend Raise<Throwable>.(TextBuffer) -> Unit) = Either.catch {
                    raise(IllegalStateException("Nested batches are not supported"))
                }
            }

            block(batchBuffer)

            when {
                accumulatedSingles.isNotEmpty() -> {
                    val data = TextEdit.Data.Batch(singles = accumulatedSingles.toList())

                    updateRope(result = currentRope to data) as? TextEdit.Data.Batch
                }

                else -> TextEdit.Data.Batch(singles = emptyList())
            }
        }
    }
}