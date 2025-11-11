package io.github.numq.haskcore.application.presentation

import io.github.numq.haskcore.application.presentation.dialog.ApplicationDialog

internal sealed interface ApplicationState {
    data object Splash : ApplicationState

    data class Content(
        val dividerPercentage: Float = .7f,
        val dialog: ApplicationDialog? = null
    ) : ApplicationState
}