package io.github.numq.haskcore.configuration.presentation

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.presentation.dialog.ConfigurationDialog

internal sealed interface ConfigurationCommand {
    data object Initialize : ConfigurationCommand

    data class UpdateConfigurations(val configurations: List<Configuration>) : ConfigurationCommand

    data class SelectConfiguration(val configuration: Configuration) : ConfigurationCommand

    data object DeselectConfiguration : ConfigurationCommand

    data class AddConfiguration(val path: String, val name: String, val command: String) : ConfigurationCommand

    data class EditConfiguration(val configuration: Configuration) : ConfigurationCommand

    data class RemoveConfiguration(val configuration: Configuration) : ConfigurationCommand

    data class OpenDialog(val dialog: ConfigurationDialog) : ConfigurationCommand

    data object CloseDialog : ConfigurationCommand

    data class OpenPopup(val popup: ConfigurationDropdownMenu) : ConfigurationCommand

    data object ClosePopup : ConfigurationCommand
}