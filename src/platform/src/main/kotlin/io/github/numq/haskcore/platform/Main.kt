package io.github.numq.haskcore.platform

import androidx.compose.ui.window.application
import io.github.numq.haskcore.entrypoint.Entrypoint

internal fun main() {
    System.setProperty("skiko.renderApi", "OPENGL")

    application {
        Entrypoint.Initialize(exitApplication = ::exitApplication)
    }
}