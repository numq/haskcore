package io.github.numq.haskcore.feature.bootstrap.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import io.github.numq.haskcore.common.presentation.font.Font
import io.github.numq.haskcore.feature.bootstrap.core.Bootstrap
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun BootstrapView(
    applicationScope: Scope,
    handleError: (Throwable) -> Unit,
    title: String,
    logo: Painter,
    exitApplication: () -> Unit,
    content: @Composable (Bootstrap, welcomeLogoFont: Font, welcomeMonoFont: Font, editorMonoFont: Font) -> Unit,
) {
    val feature = koinInject<BootstrapFeature>(scope = applicationScope)

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is BootstrapEvent.HandleFailure -> handleError(event.throwable)

                is BootstrapEvent.ExitApplication -> exitApplication()
            }
        }
    }

    when (val currentState = state) {
        is BootstrapState.Active -> Unit

        is BootstrapState.Content -> with(currentState) {
            content(bootstrap, welcomeLogoFont, welcomeMonoFont, editorMonoFont)
        }
    }
}