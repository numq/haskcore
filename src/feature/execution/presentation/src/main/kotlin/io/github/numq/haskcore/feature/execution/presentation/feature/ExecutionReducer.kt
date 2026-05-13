package io.github.numq.haskcore.feature.execution.presentation.feature

import io.github.numq.haskcore.common.presentation.feature.*
import io.github.numq.haskcore.feature.execution.core.Execution
import io.github.numq.haskcore.feature.execution.core.usecase.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ExecutionReducer(
    private val buildConfiguration: BuildConfiguration,
    private val deleteConfiguration: DeleteConfiguration,
    private val editConfiguration: EditConfiguration,
    private val observeExecution: ObserveExecution,
    private val runConfiguration: RunConfiguration,
    private val setCurrentConfiguration: SetCurrentConfiguration,
    private val stopConfiguration: StopConfiguration,
) : Reducer<ExecutionState, ExecutionCommand, ExecutionEvent> {
    override fun reduce(
        state: ExecutionState, command: ExecutionCommand,
    ): Transition<ExecutionState, ExecutionEvent> = when (command) {
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

        is ExecutionCommand.BuildConfiguration -> when (state.execution) {
            is Execution.Synced.Found.Stopped -> transition(state).effect(
                stream(key = command.key, flow = flow {
                    buildConfiguration(input = BuildConfiguration.Input(configuration = command.configuration)).fold(
                        ifLeft = ExecutionCommand::HandleFailure,
                        ifRight = { ExecutionCommand.BuildConfigurationSuccess })
                }, fallback = ExecutionCommand::HandleFailure)
            )

            else -> transition(state)
        }

        is ExecutionCommand.BuildConfigurationSuccess -> transition(state)

        is ExecutionCommand.RunConfiguration -> when (state.execution) {
            is Execution.Synced.Found.Stopped -> transition(state).effect(
                stream(key = command.key, flow = flow {
                    runConfiguration(input = RunConfiguration.Input(configuration = command.configuration)).fold(
                        ifLeft = ExecutionCommand::HandleFailure,
                        ifRight = { ExecutionCommand.RunConfigurationSuccess })
                }, fallback = ExecutionCommand::HandleFailure)
            )

            else -> transition(state)
        }

        is ExecutionCommand.RunConfigurationSuccess -> transition(state)

        is ExecutionCommand.StopConfiguration -> when (state.execution) {
            is Execution.Synced.Found.Running -> transition(state).effects(
                cancel(command.key), stream(key = command.key, flow = flow {
                    stopConfiguration(input = StopConfiguration.Input(configuration = command.configuration)).fold(
                        ifLeft = ExecutionCommand::HandleFailure,
                        ifRight = { ExecutionCommand.StopConfigurationSuccess })
                }, fallback = ExecutionCommand::HandleFailure)
            )

            else -> transition(state)
        }

        is ExecutionCommand.StopConfigurationSuccess -> transition(state)

        is ExecutionCommand.RunCurrentConfiguration -> when (val execution = state.execution) {
            is Execution.Synced.Found.Stopped -> reduce(
                state = state,
                command = ExecutionCommand.RunConfiguration(configuration = execution.currentConfiguration)
            )

            else -> transition(state)
        }

        is ExecutionCommand.StopCurrentConfiguration -> when (val execution = state.execution) {
            is Execution.Synced.Found.Running -> reduce(
                state = state,
                command = ExecutionCommand.StopConfiguration(configuration = execution.currentConfiguration)
            )

            else -> transition(state)
        }

        is ExecutionCommand.RerunConfiguration -> when (state.execution) {
            is Execution.Synced.Found.Running -> with(command) {
                val (state, events) = reduce(
                    state = state, command = ExecutionCommand.StopConfiguration(configuration = configuration)
                )

                reduce(
                    state = state, command = ExecutionCommand.RunConfiguration(configuration = configuration)
                ).events(*events.toTypedArray())
            }

            else -> transition(state)
        }

        is ExecutionCommand.SelectConfiguration -> transition(state).effect(
            action(key = command.key, fallback = ExecutionCommand::HandleFailure, block = {
                setCurrentConfiguration(input = SetCurrentConfiguration.Input(configuration = command.configuration)).fold(
                    ifLeft = ExecutionCommand::HandleFailure, ifRight = { ExecutionCommand.SelectConfigurationSuccess })
            })
        )

        is ExecutionCommand.SelectConfigurationSuccess -> transition(state)
    }
}