package io.github.numq.haskcore.service.text

import arrow.core.Either
import arrow.core.left
import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import io.github.numq.haskcore.common.core.text.*
import io.github.numq.haskcore.service.text.buffer.RopeTextBufferFactory
import io.github.numq.haskcore.service.text.buffer.TextBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalTextService(
    private val scope: CoroutineScope, private val bufferFactory: RopeTextBufferFactory,
) : TextService {
    private companion object {
        const val EDITS_BUFFER_CAPACITY = 64

        const val SPLIT_REGEX = "(?<=\\n)|(?<=\\r\\n)"
    }

    private val _textBuffer = MutableStateFlow<TextBuffer?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalForInheritanceCoroutinesApi::class)
    override val snapshot: StateFlow<TextSnapshot?> = object : StateFlow<TextSnapshot?> {
        override val value: TextSnapshot?
            get() = _textBuffer.value?.snapshot?.value

        override val replayCache: List<TextSnapshot?>
            get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<TextSnapshot?>): Nothing {
            _textBuffer.flatMapLatest { textBuffer ->
                textBuffer?.snapshot ?: flowOf(null)
            }.collect(collector)

            suspendCancellableCoroutine<Nothing> {}
        }
    }

    private val _edits = MutableSharedFlow<TextEdit>(replay = 0, extraBufferCapacity = EDITS_BUFFER_CAPACITY)

    override val edits = _edits.asSharedFlow()

    override suspend fun initialize(
        initialText: String,
    ) = bufferFactory.create(
        text = initialText,
        encoding = TextEncoding.UTF8,
        lineEnding = TextLineEnding.analyze(text = initialText).dominant
    ).map { textBuffer ->
        _textBuffer.value = textBuffer
    }

    override suspend fun execute(operation: TextOperation) = when (val textBuffer = _textBuffer.value) {
        null -> IllegalStateException("Text service is not initialized").left()

        else -> when (val revision = operation.revision) {
            textBuffer.snapshot.value.revision -> {
                with(operation.data) {
                    when (this) {
                        is TextOperation.Data.Single.Insert -> textBuffer.insert(position = position, text = text)

                        is TextOperation.Data.Single.Replace -> textBuffer.replace(range = range, text = text)

                        is TextOperation.Data.Single.Delete -> textBuffer.delete(range = range)

                        is TextOperation.Data.Batch -> textBuffer.withBatch { batch ->
                            operations.forEach { op ->
                                when (op) {
                                    is TextOperation.Data.Single.Insert -> batch.insert(
                                        position = op.position, text = op.text
                                    )

                                    is TextOperation.Data.Single.Replace -> batch.replace(
                                        range = op.range, text = op.text
                                    )

                                    is TextOperation.Data.Single.Delete -> batch.delete(range = op.range)
                                }.bind()
                            }
                        }
                    }
                }.map { data ->
                    if (data != null && !data.isEffectivelyEmpty()) {
                        val edit = when (operation) {
                            is TextOperation.User -> TextEdit.User(revision = operation.revision, data = data)

                            is TextOperation.System -> TextEdit.System(
                                revision = textBuffer.snapshot.value.revision, data = data
                            )
                        }

                        _edits.emit(edit)
                    }
                }
            }

            else -> IllegalStateException("Revision mismatch: expected ${textBuffer.snapshot.value.revision}, got ${operation.revision}").left()
        }
    }

    override suspend fun computeDifference(original: String, revised: String) = Either.catch {
        val originalLines = original.split(Regex(SPLIT_REGEX))

        val revisedLines = revised.split(Regex(SPLIT_REGEX))

        val patch = DiffUtils.diff(originalLines, revisedLines)

        val operations = patch.deltas.sortedByDescending { delta ->
            delta.source.position
        }.flatMap { delta ->
            val position = TextPosition(line = delta.source.position, column = 0)

            when (delta.type) {
                DeltaType.DELETE -> listOf(
                    TextOperation.Data.Single.Delete(
                        range = TextRange(
                            start = position, end = TextPosition(
                                line = delta.source.position + delta.source.lines.size, column = 0
                            )
                        )
                    )
                )

                DeltaType.INSERT -> listOf(
                    TextOperation.Data.Single.Insert(
                        position = position, text = delta.target.lines.joinToString("")
                    )
                )

                DeltaType.CHANGE -> listOf(
                    TextOperation.Data.Single.Delete(
                        range = TextRange(
                            start = position, end = TextPosition(
                                line = delta.source.position + delta.source.lines.size, column = 0
                            )
                        )
                    ), TextOperation.Data.Single.Insert(
                        position = position, text = delta.target.lines.joinToString("")
                    )
                )

                else -> emptyList()
            }
        }

        TextOperation.Data.Batch(operations = operations)
    }

    override fun close() {
        scope.cancel()
    }
}