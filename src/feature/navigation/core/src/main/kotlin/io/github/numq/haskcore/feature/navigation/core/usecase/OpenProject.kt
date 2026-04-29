package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.session.SessionApi
import io.github.numq.haskcore.common.core.usecase.UseCase

class OpenProject(private val sessionApi: SessionApi) : UseCase<OpenProject.Input, Unit> {
    data class Input(val path: String, val name: String?)

    override suspend fun Raise<Throwable>.execute(input: Input) = with(input) {
        sessionApi.openSessionRecord(path = path, name = name).bind()
    }
}