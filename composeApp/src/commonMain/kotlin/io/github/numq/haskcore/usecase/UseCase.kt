package io.github.numq.haskcore.usecase

interface UseCase<in Input, out Output> {
    suspend fun execute(input: Input): Result<Output>
}