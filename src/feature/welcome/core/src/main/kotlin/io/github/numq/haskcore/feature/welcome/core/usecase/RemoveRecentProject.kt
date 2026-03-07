package io.github.numq.haskcore.feature.welcome.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.service.session.SessionService

class RemoveRecentProject(private val sessionService: SessionService) : UseCase<RemoveRecentProject.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        sessionService.removeSessionRecordFromHistory(path = input.path).bind()
}