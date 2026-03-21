package io.github.numq.haskcore.service.text

import arrow.core.left
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextOperation
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.service.text.buffer.RopeTextBufferFactory
import io.github.numq.haskcore.service.text.buffer.TextBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalTextService(
    private val scope: CoroutineScope, private val bufferFactory: RopeTextBufferFactory
) : TextService {
    private companion object {
        const val EDITS_BUFFER_CAPACITY = 64
    }

    private val _textBuffer = MutableStateFlow<TextBuffer?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val snapshot: StateFlow<TextSnapshot?> = _textBuffer.flatMapLatest { textBuffer ->
        flow {
            textBuffer?.snapshot?.let { snapshots ->
                emitAll(snapshots)
            }
        }
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    private val _edits = MutableSharedFlow<TextEdit>(replay = 0, extraBufferCapacity = EDITS_BUFFER_CAPACITY)

    override val edits = _edits.asSharedFlow()

    override suspend fun initialize(initialText: String) = bufferFactory.create(text = initialText).map { textBuffer ->
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
                    if (data != null) {
                        val edit = when (operation) {
                            is TextOperation.User -> TextEdit.User(revision = revision, data = data)

                            is TextOperation.System -> TextEdit.System(revision = revision, data = data)
                        }

                        if (edit.revision == revision) {
                            _edits.emit(edit)
                        }
                    }
                }
            }

            else -> IllegalStateException("Revision mismatch: expected ${textBuffer.snapshot.value.revision}, got ${operation.revision}").left()
        }
    }

    override fun close() {
        scope.cancel()
    }
}