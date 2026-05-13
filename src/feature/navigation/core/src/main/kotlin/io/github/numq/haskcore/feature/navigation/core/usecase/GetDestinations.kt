package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.service.session.SessionService
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.navigation.core.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDestinations(private val sessionService: SessionService) : UseCase<Unit, Flow<List<Destination>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionService.session.map { session ->
        session.active.map { sessionRecord ->
            val path = sessionRecord.path

            Destination(path = path)
        }
    }
}