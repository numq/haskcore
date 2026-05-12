package io.github.numq.haskcore.feature.welcome.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.session.SessionApi
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.welcome.core.RecentProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveRecentProjects(private val sessionApi: SessionApi) : UseCase<Unit, Flow<List<RecentProject>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionApi.sessionDto.map { session ->
        session.history.map { sessionRecord ->
            RecentProject(path = sessionRecord.path, sessionRecord.name)
        }
    }
}