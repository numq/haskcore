package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.session.SessionApi
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.navigation.core.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDestinations(private val sessionApi: SessionApi) : UseCase<Unit, Flow<List<Destination>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionApi.sessionDto.map { session ->
        session.active.map { sessionRecord ->
            val path = sessionRecord.path

            Destination(path = path)
        }
    }
}