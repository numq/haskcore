package io.github.numq.haskcore.common.core.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either

sealed interface UseCase<in Input, out Output> {
    suspend fun Raise<Throwable>.execute(input: Input): Output

    suspend operator fun invoke(input: Input): Either<Throwable, Output> = either { execute(input = input) }

    interface Action : UseCase<Unit, Unit> {
        suspend fun Raise<Throwable>.action()

        override suspend fun Raise<Throwable>.execute(input: Unit): Unit = action()

        suspend operator fun invoke(): Either<Throwable, Unit> = invoke(input = Unit)
    }

    interface Query<out Output> : UseCase<Unit, Output> {
        suspend fun Raise<Throwable>.query(): Output

        override suspend fun Raise<Throwable>.execute(input: Unit): Output = query()

        suspend operator fun invoke(): Either<Throwable, Output> = invoke(input = Unit)
    }

    interface Command<in Input> : UseCase<Input, Unit> {
        suspend fun Raise<Throwable>.command(input: Input)

        override suspend fun Raise<Throwable>.execute(input: Input): Unit = command(input = input)
    }

    interface Exchange<in Input, out Output> : UseCase<Input, Output> {
        suspend fun Raise<Throwable>.exchange(input: Input): Output

        override suspend fun Raise<Throwable>.execute(input: Input): Output = exchange(input = input)
    }
}