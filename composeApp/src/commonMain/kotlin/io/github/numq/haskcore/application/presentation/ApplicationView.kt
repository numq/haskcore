package io.github.numq.haskcore.application.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import haskcore.composeapp.generated.resources.Res
import haskcore.composeapp.generated.resources.logo_black
import io.github.numq.haskcore.splash.SplashView
import io.github.numq.haskcore.status.presentation.StatusView
import io.github.numq.haskcore.theme.ApplicationTheme
import io.github.numq.haskcore.toolbar.presentation.ToolbarView
import io.github.numq.haskcore.workspace.presentation.WorkspaceView
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun ApplicationScope.ApplicationView(title: String, feature: ApplicationFeature) {
    ApplicationTheme(isDarkTheme = isSystemInDarkTheme()) {
        when (feature.state.collectAsState().value) {
            is ApplicationState.Splash -> {
                val painter = painterResource(resource = Res.drawable.logo_black)

                val size by remember(painter) {
                    derivedStateOf {
                        DpSize(
                            width = painter.intrinsicSize.width.dp + 16.dp,
                            height = painter.intrinsicSize.height.dp + 16.dp
                        )
                    }
                }

                val state = rememberWindowState(position = WindowPosition(Alignment.Center), size = size)

                Window(
                    onCloseRequest = ::exitApplication,
                    state = state,
                    title = title,
                    undecorated = true,
                    transparent = true,
                    resizable = false,
                    alwaysOnTop = true
                ) {
                    SplashView(painter = painter)
                }
            }

            is ApplicationState.Content -> {
                val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

                Window(onCloseRequest = ::exitApplication, state = windowState, title = title) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        ToolbarView(feature = koinInject {
                            val minimizeWindow = {
                                window.isMinimized = true
                            }

                            val toggleFullscreen = {
                                windowState.placement = when (windowState.placement) {
                                    WindowPlacement.Floating -> WindowPlacement.Maximized

                                    else -> WindowPlacement.Floating
                                }
                            }

                            val closeApplication = ::exitApplication

                            parametersOf(minimizeWindow, toggleFullscreen, closeApplication)
                        })

                        WorkspaceView(feature = koinInject())

                        StatusView(feature = koinInject())
                    }
                }
            }
        }
    }
}