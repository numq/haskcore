package io.github.numq.haskcore.application.presentation

import io.github.numq.haskcore.application.presentation.dialog.ApplicationDialog
import io.github.numq.haskcore.workspace.Workspace

internal sealed interface ApplicationState {
    data object Splash : ApplicationState

    data class Content(
        val activeWorkspace: Workspace = Workspace.None,
        val dividerPercentage: Float = .75f,
        val dialog: ApplicationDialog? = null
    ) : ApplicationState
}