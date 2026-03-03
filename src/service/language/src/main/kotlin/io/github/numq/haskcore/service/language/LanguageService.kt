package io.github.numq.haskcore.service.language

import arrow.core.Either
import io.github.numq.haskcore.core.text.TextRange
import io.github.numq.haskcore.service.language.semantic.SemanticToken

interface LanguageService {
    suspend fun initialize(): Either<Throwable, Unit>

    suspend fun getSemanticTokens(uri: String, range: TextRange): Either<Throwable, List<SemanticToken>>
}