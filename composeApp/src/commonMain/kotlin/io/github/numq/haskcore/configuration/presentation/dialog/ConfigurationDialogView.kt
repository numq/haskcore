package io.github.numq.haskcore.configuration.presentation.dialog

import androidx.compose.runtime.Composable
import io.github.numq.haskcore.configuration.Configuration

@Composable
internal fun ConfigurationDialogView(
    dialog: ConfigurationDialog,
    addConfiguration: (path: String, name: String, command: String) -> Unit,
    editConfiguration: (configuration: Configuration) -> Unit,
    removeConfiguration: (configuration: Configuration) -> Unit,
    close: () -> Unit
) {
    when (dialog) {
        is ConfigurationDialog.AddConfiguration -> ConfigurationDialogForm(
            title = "Add Configuration",
            initialPath = "",
            initialName = "",
            initialCommand = "",
            onSave = addConfiguration,
            onClose = close
        )

        is ConfigurationDialog.EditConfiguration -> ConfigurationDialogForm(
            title = "Edit Configuration",
            initialPath = dialog.configuration.path,
            initialName = dialog.configuration.name,
            initialCommand = dialog.configuration.command,
            onSave = { path, name, command ->
                editConfiguration(
                    dialog.configuration.copy(
                        path = path,
                        name = name,
                        command = command
                    )
                )
            },
            onClose = close
        )

        is ConfigurationDialog.RemoveConfiguration -> ConfigurationDialogConfirmation(
            title = "Remove Configuration",
            message = "Are you sure you want to remove this configuration?",
            confirmText = "Remove",
            onConfirm = { removeConfiguration(dialog.configuration) },
            onClose = close
        )
    }
}