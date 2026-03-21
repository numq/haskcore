package io.github.numq.haskcore.feature.bootstrap.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapItem
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapService
import io.github.numq.haskcore.service.syntax.initializer.SyntaxServiceInitializer
import io.github.numq.haskcore.service.session.SessionService
import kotlinx.coroutines.flow.first

class Boot(
    private val bootstrapService: BootstrapService,
    private val sessionService: SessionService,
    private val highlightingServiceInitializer: SyntaxServiceInitializer,
) : UseCase<Unit, Bootstrap> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = bootstrapService.initialize(block = {
        highlightingServiceInitializer.initialize().bind()

        val session = sessionService.session.first()

        val items = session.active.map { sessionRecord ->
            with(sessionRecord) {
                BootstrapItem(path = path, name = name, timestamp = timestamp)
            }
        }

        Bootstrap(items = items)
    }).bind()
}