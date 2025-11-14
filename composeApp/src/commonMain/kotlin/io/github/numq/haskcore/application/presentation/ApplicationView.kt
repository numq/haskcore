package io.github.numq.haskcore.application.presentation

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import haskcore.composeapp.generated.resources.Res
import haskcore.composeapp.generated.resources.logo_black
import io.github.numq.haskcore.contextmenu.DisableableContextMenuRepresentation
import io.github.numq.haskcore.editor.presentation.EditorView
import io.github.numq.haskcore.explorer.presentation.ExplorerView
import io.github.numq.haskcore.output.presentation.OutputView
import io.github.numq.haskcore.splash.SplashView
import io.github.numq.haskcore.split.VerticalSplitPane
import io.github.numq.haskcore.status.presentation.StatusView
import io.github.numq.haskcore.theme.ApplicationTheme
import io.github.numq.haskcore.toolbar.presentation.ToolbarView
import io.github.numq.haskcore.workspace.presentation.WorkspaceView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun ApplicationScope.ApplicationView(title: String, feature: ApplicationFeature) {
    ApplicationTheme(isDarkTheme = isSystemInDarkTheme()) {
        when (val state = feature.state.collectAsState().value) {
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

                val windowState = rememberWindowState(position = WindowPosition(Alignment.Center), size = size)

                Window(
                    onCloseRequest = ::exitApplication,
                    state = windowState,
                    title = title,
                    undecorated = true,
                    transparent = true,
                    resizable = false,
                    alwaysOnTop = true
                ) {
                    SplashView(painter = painter)
                }
            }

            is ApplicationState.Content -> CompositionLocalProvider(
                LocalContextMenuRepresentation provides DisableableContextMenuRepresentation
            ) {
                val coroutineScope = rememberCoroutineScope()

                val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

                Window(onCloseRequest = ::exitApplication, state = windowState, title = title, undecorated = true) {
                    val toggleFullscreen = remember(windowState.placement) {
                        {
                            windowState.placement = when (windowState.placement) {
                                WindowPlacement.Floating -> WindowPlacement.Maximized

                                else -> WindowPlacement.Floating
                            }
                        }
                    }

                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                modifier = Modifier.combinedClickable(
                                    interactionSource = null,
                                    indication = null,
                                    onDoubleClick = toggleFullscreen,
                                    onClick = {}),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 4.dp
                            ) {
                                when (windowState.placement) {
                                    WindowPlacement.Floating -> WindowDraggableArea {
                                        ToolbarView(feature = koinInject {
                                            parametersOf(
                                                { window.isMinimized = true }, toggleFullscreen, ::exitApplication
                                            )
                                        })
                                    }

                                    else -> ToolbarView(feature = koinInject {
                                        parametersOf({ window.isMinimized = true }, toggleFullscreen, ::exitApplication)
                                    })
                                }
                            }

                            VerticalSplitPane(percentage = state.dividerPercentage, onPercentageChange = { percentage ->
                                coroutineScope.launch {
                                    feature.execute(ApplicationCommand.ChangeDividerPosition(percentage = percentage))
                                }
                            }, modifier = Modifier.weight(1f), minPercentage = .6f, maxPercentage = .9f, first = {
                                WorkspaceView(feature = koinInject(), explorerContent = { workspacePath ->
                                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                                        ExplorerView(feature = koinInject {
                                            parametersOf(workspacePath)
                                        })
                                    }
                                }, editorContent = { workspacePath ->
                                    Surface(tonalElevation = 1.dp) {
                                        EditorView(feature = koinInject {
                                            parametersOf(workspacePath)
                                        })
                                    }
                                })
                            }, second = {
                                Surface(tonalElevation = 2.dp) {
                                    OutputView(feature = koinInject())
                                }
                            })

                            HorizontalDivider(modifier = Modifier.fillMaxWidth())

                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                                StatusView(feature = koinInject())
                            }
                        }
                    }
                }
            }
        }
    }
}