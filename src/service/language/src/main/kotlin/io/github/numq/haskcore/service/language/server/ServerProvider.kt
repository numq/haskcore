package io.github.numq.haskcore.service.language.server

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.lsp4j.services.LanguageServer

internal interface ServerProvider : AutoCloseable {
    val server: StateFlow<LanguageServer?>

    suspend fun initialize(hlsPath: String): Either<Throwable, Unit>
}