package io.github.numq.haskcore.feature.navigation.core.usecase

import arrow.core.getOrElse
import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.core.NavigationService
import io.github.numq.haskcore.service.session.SessionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDestinations(
    private val navigationService: NavigationService, private val sessionService: SessionService
) : UseCase<Unit, Flow<List<Destination>>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = sessionService.session.map { session ->
        session.active.map { sessionRecord ->
            val path = sessionRecord.path

            val initialWorkspaceData = navigationService.getInitialWorkspace(path = path).getOrElse { throwable ->
                println(throwable) // todo

                null
            }

            Destination(path = path, initialWorkspace = initialWorkspaceData)
        }
    }
}