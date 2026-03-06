package io.github.numq.haskcore.service.text.syntax

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextEdit
import kotlinx.coroutines.flow.SharedFlow
import org.treesitter.TSTree

internal interface SyntaxEngine {
    val treeUpdates: SharedFlow<TSTree>

    suspend fun fullParse(text: String): Either<Throwable, Unit>

    suspend fun update(text: String, edit: TextEdit): Either<Throwable, Unit>

    suspend fun <T> readTree(block: suspend (TSTree) -> T): Either<Throwable, T>
}