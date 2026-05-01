package io.github.numq.haskcore.feature.output.core.usecase

import arrow.core.raise.Raise
import io.github.numq.haskcore.api.runtime.RuntimeApi
import io.github.numq.haskcore.api.runtime.RuntimeEvent
import io.github.numq.haskcore.common.core.usecase.UseCase
import io.github.numq.haskcore.feature.output.core.Output
import io.github.numq.haskcore.feature.output.core.OutputLine
import io.github.numq.haskcore.feature.output.core.OutputService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

class ObserveOutput(
    private val outputService: OutputService, private val runtimeApi: RuntimeApi,
) : UseCase<Unit, Flow<Output>> {
    override suspend fun Raise<Throwable>.execute(input: Unit) = channelFlow {
        outputService.output.onEach(::send).launchIn(scope = this)

        runtimeApi.events.collect { event ->
            val id = event.request.id

            val name = event.request.name

            val command = event.request.command

            val arguments = event.request.arguments

            when (event) {
                is RuntimeEvent.Started -> outputService.startSession(
                    id = id, name = name, configuration = "$command ${arguments.joinToString(" ")}"
                )

                is RuntimeEvent.Stdout -> outputService.push(
                    id = id, line = OutputLine.Normal(
                        id = UUID.randomUUID().toString(), text = event.text, timestamp = event.timestamp
                    )
                )

                is RuntimeEvent.Stderr -> outputService.push(
                    id = id, line = OutputLine.Error(
                        id = UUID.randomUUID().toString(), text = event.text, timestamp = event.timestamp
                    )
                )

                is RuntimeEvent.Terminated -> outputService.stopSession(
                    id = id, exitCode = event.exitCode, duration = event.duration
                )
            }.bind()
        }
    }
}