package io.github.numq.haskcore.application.presentation

import io.github.numq.haskcore.application.presentation.dialog.ApplicationDialog

internal sealed interface ApplicationCommand {
    data object Initialize : ApplicationCommand

    data class ChangeDividerPosition(val percentage: Float) : ApplicationCommand

    data class OpenDialog(val dialog: ApplicationDialog) : ApplicationCommand

    data object CloseDialog : ApplicationCommand
}