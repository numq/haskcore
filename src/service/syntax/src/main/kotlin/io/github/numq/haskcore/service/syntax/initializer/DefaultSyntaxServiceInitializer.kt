package io.github.numq.haskcore.service.syntax.initializer

import arrow.core.Either
import io.github.numq.haskcore.service.syntax.query.SyntaxQueryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

internal class DefaultSyntaxServiceInitializer(
    private val queryProvider: SyntaxQueryProvider
) : SyntaxServiceInitializer {
    override suspend fun initialize(): Either<Throwable, Unit> = Either.catch {
        withContext(Dispatchers.IO) {
            awaitAll(
                async { queryProvider.highlightsQuery },
                async { queryProvider.localsQuery },
                async { queryProvider.injectionsQuery })
        }
    }
}