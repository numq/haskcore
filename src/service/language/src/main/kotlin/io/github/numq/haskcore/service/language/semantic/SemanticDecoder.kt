package io.github.numq.haskcore.service.language.semantic

import arrow.core.Either

internal interface SemanticDecoder {
    suspend fun decode(data: List<Int>, legend: List<LegendType>): Either<Throwable, List<SemanticToken>>
}