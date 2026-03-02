package io.github.numq.haskcore.service.clipboard

import arrow.core.Either
import kotlinx.coroutines.flow.StateFlow

interface ClipboardService {
    val clipboard: StateFlow<Clipboard>

    suspend fun copyToClipboard(text: String): Either<Throwable, Unit>
}