package io.github.numq.haskcore.service.text

import arrow.core.Either
import io.github.numq.haskcore.service.text.syntax.QueryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

internal class DefaultTextServiceInitializer(private val queryProvider: QueryProvider) : TextServiceInitializer {
    override suspend fun initialize(): Either<Throwable, Unit> = Either.catch {
        withContext(Dispatchers.IO) {
            awaitAll(
                async { queryProvider.highlightsQuery },
                async { queryProvider.localsQuery },
                async { queryProvider.injectionsQuery })
        }
    }
}