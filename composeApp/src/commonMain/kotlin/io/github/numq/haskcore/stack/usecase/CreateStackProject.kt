package io.github.numq.haskcore.stack.usecase

import io.github.numq.haskcore.output.OutputMessage
import io.github.numq.haskcore.output.OutputRepository
import io.github.numq.haskcore.output.OutputSession
import io.github.numq.haskcore.stack.StackService
import io.github.numq.haskcore.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal class CreateStackProject(
    private val stackService: StackService,
    private val outputRepository: OutputRepository,
) : UseCase<CreateStackProject.Input, Flow<Unit>> {
    data class Input(val path: String, val name: String, val template: String?, val bare: Boolean)

    override suspend fun execute(input: Input) = with(input) {
        stackService.createProject(
            path = path, name = name, template = template, bare = bare
        ).mapCatching { stackResult ->
            val session = OutputSession(name = stackResult.command)

            try {
                stackResult.messages.onStart {
                    outputRepository.startSession(session = session, unique = true).getOrThrow()
                }.map { message ->
                    outputRepository.addMessage(sessionId = session.id, message = message).getOrThrow()
                }
            } catch (throwable: Throwable) {
                outputRepository.addMessage(
                    sessionId = session.id, message = OutputMessage.Error(
                        text = "💥 Operation failed: ${throwable.message}",
                    )
                ).getOrThrow()

                throw throwable
            }
        }
    }
}