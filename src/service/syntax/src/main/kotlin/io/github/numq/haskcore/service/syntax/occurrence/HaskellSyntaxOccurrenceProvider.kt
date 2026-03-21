package io.github.numq.haskcore.service.syntax.occurrence

import arrow.core.raise.either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.service.syntax.symbol.Symbol
import io.github.numq.haskcore.service.syntax.symbol.SymbolTable

internal class HaskellSyntaxOccurrenceProvider(private val symbolTable: SymbolTable) : SyntaxOccurrenceProvider {
    override suspend fun getSyntaxOccurrences(position: TextPosition) = either {
        val target = symbolTable.findByPosition(
            position = position
        ).bind() ?: return@either emptyList<SyntaxOccurrence>()

        val symbols = symbolTable.findByName(name = target.name).bind()

        symbols.map { symbol ->
            val range = symbol.range

            when (symbol) {
                is Symbol.Definition -> SyntaxOccurrence.Definition(range = range)

                is Symbol.Reference -> SyntaxOccurrence.Reference(range = range)
            }
        }
    }
}