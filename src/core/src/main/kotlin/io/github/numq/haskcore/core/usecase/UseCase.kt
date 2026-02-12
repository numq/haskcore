package io.github.numq.haskcore.core.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either

interface UseCase<in Input, out Output> {
    suspend fun Raise<Throwable>.execute(input: Input): Output

    suspend operator fun invoke(input: Input): Either<Throwable, Output> = either { execute(input) }
}