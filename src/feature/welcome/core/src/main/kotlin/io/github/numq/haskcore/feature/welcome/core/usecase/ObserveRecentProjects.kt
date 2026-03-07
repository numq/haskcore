package io.github.numq.haskcore.feature.welcome.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.welcome.core.RecentProject
import io.github.numq.haskcore.service.session.SessionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveRecentProjects(private val sessionService: SessionService) : UseCase<Unit, Flow<List<RecentProject>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionService.session.map { session ->
        session.history.map { sessionRecord ->
            RecentProject(path = sessionRecord.path, sessionRecord.name)
        }
    }
}