package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.session.SessionService

class CloseWorkspace(private val path: String, private val sessionService: SessionService) : UseCase.Action {
    override suspend fun Raise<Throwable>.action() = sessionService.closeSessionRecord(path = path).bind()
}