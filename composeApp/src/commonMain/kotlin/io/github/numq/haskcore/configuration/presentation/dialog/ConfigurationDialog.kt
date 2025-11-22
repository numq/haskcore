package io.github.numq.haskcore.configuration.presentation.dialog

import io.github.numq.haskcore.configuration.Configuration

internal sealed interface ConfigurationDialog {

    data object AddConfiguration : ConfigurationDialog

    data class EditConfiguration(val configuration: Configuration) : ConfigurationDialog

    data class RemoveConfiguration(val configuration: Configuration) : ConfigurationDialog
}