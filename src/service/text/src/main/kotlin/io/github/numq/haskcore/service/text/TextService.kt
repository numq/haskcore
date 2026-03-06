package io.github.numq.haskcore.service.text

import arrow.core.Either
import io.github.numq.haskcore.core.text.*
import io.github.numq.haskcore.service.text.occurrence.Occurrence
import io.github.numq.haskcore.service.text.syntax.SyntaxScope
import io.github.numq.haskcore.service.text.syntax.SyntaxToken
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TextService : AutoCloseable {
    val edits: SharedFlow<TextEdit?>

    val snapshot: StateFlow<TextSnapshot?>

    suspend fun initialize(initialText: String): Either<Throwable, Unit>

    suspend fun applyEdit(edit: TextEdit): Either<Throwable, Unit>

    suspend fun getScopes(range: TextRange): Either<Throwable, List<SyntaxScope>>

    suspend fun getSyntaxTokens(range: TextRange): Either<Throwable, List<SyntaxToken>>

    suspend fun getLocalOccurrences(position: TextPosition): Either<Throwable, List<Occurrence>>

    suspend fun execute(operation: TextOperation): Either<Throwable, Unit>
}