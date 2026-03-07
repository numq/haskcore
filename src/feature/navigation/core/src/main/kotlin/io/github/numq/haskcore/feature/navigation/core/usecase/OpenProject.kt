package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.service.session.SessionService

class OpenProject(private val sessionService: SessionService) : UseCase<OpenProject.Input, Unit> {
    data class Input(val path: String, val name: String?)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        sessionService.openSessionRecord(path = path, name = name).bind()
    }
}