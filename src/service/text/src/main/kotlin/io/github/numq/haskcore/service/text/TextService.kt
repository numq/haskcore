package io.github.numq.haskcore.service.text

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextOperation
import io.github.numq.haskcore.core.text.TextSnapshot
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TextService : AutoCloseable {
    val snapshot: StateFlow<TextSnapshot?>

    val edits: SharedFlow<TextEdit?>

    suspend fun initialize(initialText: String): Either<Throwable, Unit>

    suspend fun execute(operation: TextOperation): Either<Throwable, Unit>
}