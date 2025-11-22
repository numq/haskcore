package io.github.numq.haskcore.configuration.presentation

import io.github.numq.haskcore.configuration.Configuration
import io.github.numq.haskcore.configuration.presentation.dialog.ConfigurationDialog

internal data class ConfigurationState(
    val configurations: List<Configuration> = emptyList(),
    val selectedConfiguration: Configuration? = null,
    val dialog: ConfigurationDialog? = null,
    val dropdownMenu: ConfigurationDropdownMenu? = null
)