package io.github.numq.haskcore.feature.execution.presentation

import io.github.numq.haskcore.core.feature.*
import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.usecase.ObserveExecution
import io.github.numq.haskcore.feature.execution.core.usecase.SelectArtifact
import io.github.numq.haskcore.feature.execution.core.usecase.StartExecution
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ExecutionReducer(
    private val observeExecution: ObserveExecution,
    private val startExecution: StartExecution,
    private val selectArtifact: SelectArtifact,
) : Reducer<ExecutionState, ExecutionCommand, ExecutionEvent> {
    override fun reduce(state: ExecutionState, command: ExecutionCommand) = when (command) {
        is ExecutionCommand.HandleFailure -> transition(state).event(ExecutionEvent.HandleFailure(throwable = command.throwable))

        is ExecutionCommand.ObserveExecution -> transition(state).effect(
            action(
                key = command.key, fallback = ExecutionCommand::HandleFailure, block = {
                    observeExecution(input = Unit).fold(
                        ifLeft = ExecutionCommand::HandleFailure, ifRight = ExecutionCommand::ObserveExecutionSuccess
                    )
                })
        )

        is ExecutionCommand.ObserveExecutionSuccess -> transition(state).effect(
            stream(
                key = command.key,
                flow = command.flow.map(ExecutionCommand::UpdateExecution),
                fallback = ExecutionCommand::HandleFailure
            )
        )

        is ExecutionCommand.UpdateExecution -> transition(state.copy(execution = command.execution))

        is ExecutionCommand.Run -> when (val execution = state.execution) {
            is Execution.Synced.Found.Stopped -> transition(state).effect(
                stream(key = command.key, flow = flow {
                    startExecution(input = StartExecution.Input(artifact = execution.selectedArtifact)).fold(
                        ifLeft = ExecutionCommand::HandleFailure, ifRight = { ExecutionCommand.RunSuccess })
                }, fallback = ExecutionCommand::HandleFailure)
            )

            else -> transition(state)
        }

        is ExecutionCommand.RunSuccess -> transition(state)

        is ExecutionCommand.Stop -> when (state.execution) {
            is Execution.Synced.Found.Running -> transition(state).effect(cancel(command.key))

            else -> transition(state)
        }

        is ExecutionCommand.StopSuccess -> transition(state)

        is ExecutionCommand.SelectArtifact -> transition(state).effect(
            action(key = command.key, fallback = ExecutionCommand::HandleFailure, block = {
                selectArtifact(input = SelectArtifact.Input(artifact = command.artifact)).fold(
                    ifLeft = ExecutionCommand::HandleFailure, ifRight = { ExecutionCommand.SelectArtifactSuccess })
            })
        )

        is ExecutionCommand.SelectArtifactSuccess -> transition(state)
    }
}