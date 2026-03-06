package io.github.numq.haskcore.service.text

import arrow.core.Either
import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextOperation
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.text.buffer.TextBuffer
import io.github.numq.haskcore.service.text.buffer.TextBufferFactory
import io.github.numq.haskcore.service.text.occurrence.OccurrenceProvider
import io.github.numq.haskcore.service.text.syntax.ScopeProvider
import io.github.numq.haskcore.service.text.syntax.SyntaxEngine
import io.github.numq.haskcore.service.text.syntax.SyntaxTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

internal class LocalTextService(
    private val scope: CoroutineScope,
    private val syntaxEngine: SyntaxEngine,
    private val occurrenceProvider: OccurrenceProvider,
    private val scopeProvider: ScopeProvider,
    private val syntaxTokenProvider: SyntaxTokenProvider,
    private val bufferFactory: TextBufferFactory
) : TextService {
    private companion object {
        const val EDITS_BUFFER_CAPACITY = 64
    }

    private val _buffer = MutableStateFlow<TextBuffer?>(null)

    private val _edits = MutableSharedFlow<TextEdit>(replay = 0, extraBufferCapacity = EDITS_BUFFER_CAPACITY)

    override val edits = _edits.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val snapshot = _buffer.flatMapLatest { buffer ->
        buffer?.snapshot ?: flowOf(null)
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    private suspend fun <T> withBuffer(block: suspend (TextBuffer) -> Either<Throwable, T>) =
        _buffer.value?.let { buffer ->
            block(buffer)
        } ?: Either.Left(IllegalStateException("TextService is not initialized"))

    override suspend fun initialize(initialText: String) = either {
        val buffer = bufferFactory.create(text = initialText).bind()

        syntaxEngine.fullParse(text = initialText).bind()

        _buffer.value = buffer
    }

    override suspend fun applyEdit(edit: TextEdit) = withBuffer { buffer ->
        _edits.emit(edit)

        syntaxEngine.update(text = buffer.snapshot.value.text, edit = edit)
    }

    override suspend fun getScopes(range: TextRange) = withBuffer {
        scopeProvider.getScopes(range = range)
    }

    override suspend fun getSyntaxTokens(range: TextRange) = withBuffer {
        syntaxTokenProvider.getSyntaxTokens(range = range)
    }

    override suspend fun getLocalOccurrences(position: TextPosition) = withBuffer { buffer ->
        occurrenceProvider.getLocalOccurrences(snapshot = buffer.snapshot.value, position = position)
    }

    override suspend fun execute(operation: TextOperation) = withBuffer { buffer ->
        either {
            val data = with(operation.data) {
                when (this) {
                    is TextOperation.Data.Single.Insert -> buffer.insert(position = position, text = text)

                    is TextOperation.Data.Single.Replace -> buffer.replace(range = range, text = text)

                    is TextOperation.Data.Single.Delete -> buffer.delete(range = range)

                    is TextOperation.Data.Batch -> buffer.withBatch { batch ->
                        operations.forEach { op ->
                            when (op) {
                                is TextOperation.Data.Single.Insert -> batch.insert(
                                    position = op.position, text = op.text
                                )

                                is TextOperation.Data.Single.Replace -> batch.replace(range = op.range, text = op.text)

                                is TextOperation.Data.Single.Delete -> batch.delete(range = op.range)
                            }.bind()
                        }
                    }
                }.bind()
            }

            if (data != null) {
                val revision = buffer.snapshot.value.revision

                val edit = when (operation) {
                    is TextOperation.User -> TextEdit.User(data = data, revision = revision)

                    is TextOperation.System -> TextEdit.System(data = data, revision = revision)
                }

                _edits.emit(edit)

                syntaxEngine.update(text = buffer.snapshot.value.text, edit = edit).bind()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}