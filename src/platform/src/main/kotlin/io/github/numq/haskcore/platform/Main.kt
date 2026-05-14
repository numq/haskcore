package io.github.numq.haskcore.platform

import androidx.compose.ui.window.application
import io.github.numq.haskcore.entrypoint.Entrypoint
import kotlin.system.exitProcess

internal fun main() = when {
    Instance.isFirst -> {
        System.setProperty("skiko.renderApi", "OPENGL")

        application {
            Entrypoint.Initialize(exitApplication = ::exitApplication)
        }
    }

    else -> exitProcess(0)
}