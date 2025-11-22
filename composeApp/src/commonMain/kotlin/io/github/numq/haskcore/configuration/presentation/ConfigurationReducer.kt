package io.github.numq.haskcore.configuration.presentation

import io.github.numq.haskcore.configuration.usecase.AddConfiguration
import io.github.numq.haskcore.configuration.usecase.EditConfiguration
import io.github.numq.haskcore.configuration.usecase.ObserveConfigurations
import io.github.numq.haskcore.configuration.usecase.RemoveConfiguration
import io.github.numq.haskcore.feature.Event
import io.github.numq.haskcore.feature.Reducer

internal class ConfigurationReducer(
    private val addConfiguration: AddConfiguration,
    private val editConfiguration: EditConfiguration,
    private val observeConfigurations: ObserveConfigurations,
    private val removeConfiguration: RemoveConfiguration,
) : Reducer<ConfigurationCommand, ConfigurationState> {
    override suspend fun reduce(
        state: ConfigurationState, command: ConfigurationCommand
    ) = when (command) {
        is ConfigurationCommand.Initialize -> observeConfigurations.execute(input = Unit).fold(onSuccess = { flow ->
            transition(state, ConfigurationEvent.ObserveConfigurations(flow = flow))
        }, onFailure = { throwable ->
            transition(state, Event.Failure(throwable = throwable))
        })

        is ConfigurationCommand.UpdateConfigurations -> transition(
            state.copy(
                configurations = command.configurations,
                selectedConfiguration = command.configurations.find { thisConfiguration ->
                    thisConfiguration.id == state.selectedConfiguration?.id
                })
        )

        is ConfigurationCommand.SelectConfiguration -> when (state.dropdownMenu) {
            is ConfigurationDropdownMenu.Configurations -> transition(
                state.copy(
                    dropdownMenu = null, selectedConfiguration = command.configuration
                )
            )

            else -> transition(state)
        }

        is ConfigurationCommand.DeselectConfiguration -> transition(state.copy(selectedConfiguration = null))

        is ConfigurationCommand.AddConfiguration -> addConfiguration.execute(
            input = AddConfiguration.Input(
                path = command.path, name = command.name, command = command.command
            )
        ).fold(onSuccess = { configuration ->
            transition(state.copy(dialog = null, selectedConfiguration = configuration))
        }, onFailure = { throwable ->
            transition(state.copy(dialog = null), Event.Failure(throwable = throwable))
        })

        is ConfigurationCommand.EditConfiguration -> editConfiguration.execute(
            input = EditConfiguration.Input(configuration = command.configuration)
        ).fold(onSuccess = { configuration ->
            val selectedConfiguration = when (state.selectedConfiguration?.id) {
                configuration.id -> configuration

                else -> state.selectedConfiguration
            }

            transition(state.copy(dialog = null, selectedConfiguration = selectedConfiguration))
        }, onFailure = { throwable ->
            transition(state.copy(dialog = null), Event.Failure(throwable = throwable))
        })

        is ConfigurationCommand.RemoveConfiguration -> removeConfiguration.execute(
            input = RemoveConfiguration.Input(configuration = command.configuration)
        ).fold(onSuccess = {
            transition(state.copy(dialog = null))
        }, onFailure = { throwable ->
            transition(state.copy(dialog = null), Event.Failure(throwable = throwable))
        })

        is ConfigurationCommand.OpenDialog -> transition(state.copy(dialog = command.dialog))

        is ConfigurationCommand.CloseDialog -> transition(state.copy(dialog = null))

        is ConfigurationCommand.OpenPopup -> transition(state.copy(dropdownMenu = command.popup))

        is ConfigurationCommand.ClosePopup -> transition(state.copy(dropdownMenu = null))
    }
}