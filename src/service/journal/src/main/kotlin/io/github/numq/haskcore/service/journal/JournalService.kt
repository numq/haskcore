package io.github.numq.haskcore.service.journal

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextRevision
import kotlinx.coroutines.flow.StateFlow

interface JournalService : AutoCloseable {
    val journal: StateFlow<Journal>

    suspend fun push(edit: TextEdit.User): Either<Throwable, Unit>

    suspend fun undo(revision: TextRevision): Either<Throwable, TextEdit?>

    suspend fun redo(revision: TextRevision): Either<Throwable, TextEdit?>

    suspend fun clear(): Either<Throwable, Unit>
}