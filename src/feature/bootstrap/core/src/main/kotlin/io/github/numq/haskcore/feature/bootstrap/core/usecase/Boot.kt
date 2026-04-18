package io.github.numq.haskcore.feature.bootstrap.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.session.SessionApi
import io.github.numq.haskcore.api.syntax.initializer.SyntaxServiceInitializer
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapItem
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapService
import kotlinx.coroutines.flow.first

class Boot(
    private val sessionApi: SessionApi,
    private val bootstrapService: BootstrapService,
    private val highlightingServiceInitializer: SyntaxServiceInitializer,
) : UseCase<Unit, Bootstrap> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = bootstrapService.initialize(block = {
        highlightingServiceInitializer.initialize().bind()

        val session = sessionApi.sessionDto.first()

        val items = session.active.map { sessionRecord ->
            with(sessionRecord) {
                BootstrapItem(path = path, name = name, timestamp = timestamp)
            }
        }

        Bootstrap(items = items)
    }).bind()
}