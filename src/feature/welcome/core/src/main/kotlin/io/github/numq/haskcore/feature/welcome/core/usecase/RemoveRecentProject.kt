package io.github.numq.haskcore.feature.welcome.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.session.SessionService

class RemoveRecentProject(private val sessionService: SessionService) : UseCase.Command<RemoveRecentProject.Input> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.command(input: Input) =
        sessionService.removeSessionRecordFromHistory(path = input.path).bind()
}