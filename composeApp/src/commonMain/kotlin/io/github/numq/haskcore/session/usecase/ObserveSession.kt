package io.github.numq.haskcore.session.usecase

import io.github.numq.haskcore.session.Session
import io.github.numq.haskcore.session.SessionRepository
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveSession(
    private val sessionRepository: SessionRepository
) : UseCase<Unit, Flow<Session>> {
    override suspend fun execute(input: Unit) = Result.success(sessionRepository.session)
}