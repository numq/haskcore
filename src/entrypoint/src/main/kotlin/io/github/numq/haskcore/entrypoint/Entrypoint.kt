package io.github.numq.haskcore.entrypoint

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.text.TextEncoding
import io.github.numq.haskcore.common.core.text.TextLineEnding
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.presentation.theme.application.ApplicationTheme
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapView
import io.github.numq.haskcore.feature.editor.presentation.feature.view.EditorView
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionView
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerFeature
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerView
import io.github.numq.haskcore.feature.log.presentation.feature.LogView
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationView
import io.github.numq.haskcore.feature.output.presentation.feature.OutputFeature
import io.github.numq.haskcore.feature.output.presentation.feature.OutputView
import io.github.numq.haskcore.feature.status.presentation.feature.StatusView
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeView
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceView
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object Entrypoint {
    private const val APPLICATION_NAME = "haskcore"

    private const val USER_HOME = "user.home"

    private val applicationPath = Path.of(System.getProperty(USER_HOME)).absolutePathString()

    init {
        startKoin {
            allowOverride(false)

            modules(applicationModule)
        }
    }

    @Composable
    fun Initialize(exitApplication: () -> Unit) {
        val koin = getKoin()

        val applicationScope = remember {
            val qualifier = ScopeQualifier.Application

            koin.getOrCreateScope(
                scopeId = applicationPath, qualifier = qualifier, source = applicationPath
            ).apply {
                declare(instance = applicationPath, qualifier = qualifier)
            }
        }

        DisposableEffect(applicationScope.id) {
            onDispose {
                applicationScope.close()
            }
        }

        val logo = painterResource("drawable/logo.svg")

        val isDark = isSystemInDarkTheme()

        val editorTheme = koinInject<EditorTheme>(scope = applicationScope) { parametersOf(isDark) }

        val layerFactory = koinInject<LayerFactory>(scope = applicationScope)

        ApplicationTheme(isDark = isDark) {
            BootstrapView(
                applicationScope = applicationScope,
                handleError = Throwable::printStackTrace,
                title = APPLICATION_NAME,
                logo = logo,
                exitApplication = exitApplication,
                content = { bootstrap, welcomeLogoFont, welcomeMonoFont, editorMonoFont ->
                    NavigationView(
                        applicationScope = applicationScope,
                        handleError = Throwable::printStackTrace,
                        initialDestinations = bootstrap.items.map { item ->
                            Destination(path = item.path)
                        },
                        welcome = { openProject ->
                            WelcomeView(
                                applicationScope = applicationScope,
                                handleError = Throwable::printStackTrace,
                                title = APPLICATION_NAME,
                                logo = logo,
                                logoFont = welcomeLogoFont,
                                monoFont = welcomeMonoFont,
                                openProject = openProject,
                                exitApplication = exitApplication
                            )
                        },
                        workspace = { destination ->
                            val projectScope = remember(applicationScope.id, destination.path) {
                                val projectPath = destination.path

                                val qualifier = ScopeQualifier.Project

                                koin.getOrCreateScope(
                                    scopeId = projectPath, qualifier = qualifier, source = projectPath
                                ).apply {
                                    linkTo(applicationScope)

                                    declare(instance = projectPath, qualifier = qualifier)
                                }
                            }

                            DisposableEffect(projectScope.id) {
                                onDispose {
                                    projectScope.close()
                                }
                            }

                            val explorerFeature = koinInject<ExplorerFeature>(scope = projectScope)

                            val outputFeature = koinInject<OutputFeature>(scope = projectScope)

                            val outputState by outputFeature.state.collectAsState()

                            val hasOutput = remember(outputState.output) {
                                with(outputState.output) {
                                    sessions.isNotEmpty() && activeSession != null
                                }
                            }

                            val (textPosition, setTextPosition) = remember { mutableStateOf<TextPosition?>(null) }

                            val (textEncoding, setTextEncoding) = remember { mutableStateOf<TextEncoding?>(null) }

                            val (textLineEnding, setTextLineEnding) = remember { mutableStateOf<TextLineEnding?>(null) }

                            WorkspaceView(
                                projectScope = projectScope,
                                handleError = Throwable::printStackTrace,
                                logo = logo,
                                exitApplication = exitApplication,
                                execution = {
                                    ExecutionView(projectScope = projectScope, handleError = Throwable::printStackTrace)
                                },
                                explorer = {
                                    ExplorerView(feature = explorerFeature, handleError = Throwable::printStackTrace)
                                },
                                log = {
                                    LogView(projectScope = projectScope, handleError = Throwable::printStackTrace)
                                },
                                editor = { path, language ->
                                    EditorView(
                                        projectScope = projectScope,
                                        handleError = Throwable::printStackTrace,
                                        path = path,
                                        language = language,
                                        font = editorMonoFont,
                                        theme = editorTheme,
                                        layerFactory = layerFactory,
                                        onTextPosition = setTextPosition,
                                        onTextLineEnding = setTextLineEnding,
                                        onTextEncoding = setTextEncoding,
                                    )
                                },
                                output = when {
                                    hasOutput -> {
                                        @Composable {
                                            OutputView(
                                                projectScope = projectScope, handleError = Throwable::printStackTrace
                                            )
                                        }
                                    }

                                    else -> null
                                },
                                status = {
                                    StatusView(
                                        projectScope = projectScope,
                                        handleError = Throwable::printStackTrace,
                                        textPosition = textPosition,
                                        textLineEnding = textLineEnding,
                                        textEncoding = textEncoding,
                                        navigateToPath = { path ->
                                            explorerFeature.execute(ExplorerCommand.OpenPath(path = path))
                                        })
                                })
                        })
                })
        }
    }
}