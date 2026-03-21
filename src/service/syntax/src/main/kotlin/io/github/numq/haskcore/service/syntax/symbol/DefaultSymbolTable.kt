package io.github.numq.haskcore.service.syntax.symbol

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextRange
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultSymbolTable : SymbolTable {
    private val mutex = Mutex()

    private val nameIndex = mutableMapOf<String, MutableList<Symbol>>()

    private val spatialIndex = mutableListOf<Symbol>()

    override suspend fun findByName(name: String) = Either.catch {
        mutex.withLock {
            nameIndex[name] ?: emptyList()
        }
    }

    override suspend fun findByPosition(position: TextPosition) = Either.catch {
        mutex.withLock {
            spatialIndex.find { symbol ->
                symbol.range.contains(position)
            }
        }
    }

    override suspend fun add(symbol: Symbol) = Either.catch {
        mutex.withLock {
            nameIndex.getOrPut(symbol.name) { mutableListOf() }.add(symbol)

            val index = spatialIndex.binarySearch { thisSymbol ->
                symbol.range.start.compareTo(thisSymbol.range.start)
            }

            val insertAt = when {
                index < 0 -> -(index + 1)

                else -> index
            }

            spatialIndex.add(insertAt, symbol)
        }
    }

    override suspend fun removeInRange(range: TextRange) = Either.catch {
        mutex.withLock {
            spatialIndex.removeIf { symbol ->
                symbol.range.intersects(range)
            }

            nameIndex.values.forEach { list ->
                list.removeIf { symbol ->
                    symbol.range.intersects(range)
                }
            }

            nameIndex.values.removeIf(List<Symbol>::isEmpty)

            Unit
        }
    }

    override suspend fun clear() = Either.catch {
        mutex.withLock {
            nameIndex.clear()

            spatialIndex.clear()
        }
    }
}