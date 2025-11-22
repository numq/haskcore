package io.github.numq.haskcore.configuration.presentation

internal sealed interface ConfigurationDropdownMenu {
    data object Configurations : ConfigurationDropdownMenu

    data object Actions : ConfigurationDropdownMenu
}