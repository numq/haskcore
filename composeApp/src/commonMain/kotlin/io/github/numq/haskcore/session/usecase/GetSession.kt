package io.github.numq.haskcore.session.usecase

import io.github.numq.haskcore.session.Session
import io.github.numq.haskcore.session.SessionRepository
import io.github.numq.haskcore.usecase.UseCase

internal class GetSession(
    private val sessionRepository: SessionRepository
) : UseCase<Unit, Session> {
    override suspend fun execute(input: Unit) = Result.success(sessionRepository.session.value)
}