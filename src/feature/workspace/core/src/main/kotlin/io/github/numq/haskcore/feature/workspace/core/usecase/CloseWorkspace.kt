package io.github.numq.haskcore.feature.workspace.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.service.session.SessionService
import io.github.numq.haskcore.common.core.usecase.UseCase

class CloseWorkspace(
    private val path: String, private val sessionService: SessionService,
) : UseCase<Unit, Unit> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionService.closeSessionRecord(path = path).bind()
}