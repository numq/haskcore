package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.service.session.SessionService

class OpenProject(private val sessionService: SessionService) : UseCase.Command<OpenProject.Input> {
    data class Input(val path: String, val name: String?)

    override suspend fun Raise<Throwable>.command(input: Input) = with(input) {
        sessionService.openSessionRecord(path = path, name = name).bind()
    }
}