package io.github.numq.haskcore.service.syntax.symbol

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange

internal interface SymbolTable {
    suspend fun findByName(name: String): Either<Throwable, List<Symbol>>

    suspend fun findByPosition(position: TextPosition): Either<Throwable, Symbol?>

    suspend fun add(symbol: Symbol): Either<Throwable, Unit>

    suspend fun removeInRange(range: TextRange): Either<Throwable, Unit>

    suspend fun clear(): Either<Throwable, Unit>
}