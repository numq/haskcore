package io.github.numq.haskcore.feature.bootstrap.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.core.usecase.UseCase
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapItem
import io.github.numq.haskcore.feature.bootstrap.core.BootstrapService
import io.github.numq.haskcore.service.session.SessionService
import io.github.numq.haskcore.service.text.TextServiceInitializer
import kotlinx.coroutines.flow.first

class Boot(
    private val textServiceInitializer: TextServiceInitializer,
    private val bootstrapService: BootstrapService,
    private val sessionService: SessionService,
) : UseCase<Unit, Bootstrap> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = bootstrapService.initialize(block = {
        textServiceInitializer.initialize().bind()

        val session = sessionService.session.first()

        val items = session.active.map { sessionRecord ->
            BootstrapItem(path = sessionRecord.path, name = sessionRecord.name, timestamp = sessionRecord.timestamp)
        }

        Bootstrap(items = items)
    }).bind()
}