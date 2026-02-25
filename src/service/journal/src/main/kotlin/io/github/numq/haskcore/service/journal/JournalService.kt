package io.github.numq.haskcore.service.journal

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import kotlinx.coroutines.flow.StateFlow

interface JournalService : AutoCloseable {
    val journal: StateFlow<Journal>

    suspend fun push(edit: TextEdit.User): Either<Throwable, Unit>

    suspend fun undo(revision: Long): Either<Throwable, TextEdit?>

    suspend fun redo(revision: Long): Either<Throwable, TextEdit?>
}