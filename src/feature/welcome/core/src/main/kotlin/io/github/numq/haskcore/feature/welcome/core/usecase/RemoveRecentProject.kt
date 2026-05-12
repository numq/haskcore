package io.github.numq.haskcore.feature.welcome.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.session.SessionApi
import io.github.numq.haskcore.common.core.usecase.UseCase

class RemoveRecentProject(private val sessionApi: SessionApi) : UseCase<RemoveRecentProject.Input, Unit> {
    data class Input(val path: String)

    override suspend fun Raise<Throwable>.execute(input: Input) =
        sessionApi.removeSessionRecordFromHistory(path = input.path).bind()
}