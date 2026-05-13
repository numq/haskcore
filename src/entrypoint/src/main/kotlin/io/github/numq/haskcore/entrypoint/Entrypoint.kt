package io.github.numq.haskcore.entrypoint

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.presentation.container.BackgroundContainer
import io.github.numq.haskcore.common.presentation.container.ForegroundContainer
import io.github.numq.haskcore.common.presentation.feature.Feature
import io.github.numq.haskcore.common.presentation.font.DefaultFontParameters
import io.github.numq.haskcore.common.presentation.font.EditorFont
import io.github.numq.haskcore.common.presentation.font.LogoFont
import io.github.numq.haskcore.common.presentation.font.MonoFont
import io.github.numq.haskcore.common.presentation.theme.application.ApplicationTheme
import io.github.numq.haskcore.common.presentation.theme.editor.EditorTheme
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapView
import io.github.numq.haskcore.feature.editor.presentation.feature.view.EditorView
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionView
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerView
import io.github.numq.haskcore.feature.log.presentation.feature.LogView
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationView
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

        val welcomeLogoFont = koinInject<LogoFont>(scope = applicationScope) {
            parametersOf(
                96f, DefaultFontParameters.DEFAULT_LINE_SPACING
            )
        }

        val welcomeMonoFont = koinInject<MonoFont>(scope = applicationScope) {
            parametersOf(
                32f, DefaultFontParameters.DEFAULT_LINE_SPACING
            )
        }

        val editorFont = koinInject<EditorFont>(scope = applicationScope) {
            parametersOf(
                DefaultFontParameters.DEFAULT_SIZE, DefaultFontParameters.DEFAULT_LINE_SPACING
            )
        }

        val icon = painterResource("drawable/icon.svg")

        val isDark = isSystemInDarkTheme()

        val editorTheme = koinInject<EditorTheme>(scope = applicationScope) { parametersOf(isDark) }

        val layerFactory = koinInject<LayerFactory>(scope = applicationScope)

        ApplicationTheme(isDark = isDark) {
            BootstrapView(
                applicationScope = applicationScope,
                handleError = Throwable::printStackTrace,
                title = APPLICATION_NAME,
                icon = icon,
                exitApplication = exitApplication,
                content = { bootstrap ->
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
                                icon = icon,
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

                            val explorerFeature = koinInject<Feature<ExplorerState, ExplorerCommand, ExplorerEvent>>(
                                scope = projectScope
                            )

                            WorkspaceView(
                                projectScope = projectScope,
                                handleError = Throwable::printStackTrace,
                                icon = icon,
                                background = { content ->
                                    BackgroundContainer {
                                        content()
                                    }
                                },
                                execution = {
                                    ExecutionView(
                                        projectScope = projectScope, handleError = Throwable::printStackTrace
                                    )
                                },
                                explorer = { path ->
                                    ForegroundContainer {
                                        ExplorerView(
                                            feature = explorerFeature,
                                            handleError = Throwable::printStackTrace,
                                            selectedPath = path
                                        )
                                    }
                                },
                                log = {
                                    ForegroundContainer {
                                        LogView(
                                            projectScope = projectScope, handleError = Throwable::printStackTrace
                                        )
                                    }
                                },
                                editor = { path, tabs ->
                                    ForegroundContainer {
                                        tabs {
                                            EditorView(
                                                projectScope = projectScope,
                                                handleError = Throwable::printStackTrace,
                                                path = path,
                                                font = editorFont,
                                                theme = editorTheme,
                                                layerFactory = layerFactory,
                                            )
                                        }
                                    }
                                },
                                output = {
                                    ForegroundContainer {
                                        OutputView(
                                            projectScope = projectScope, handleError = Throwable::printStackTrace
                                        )
                                    }
                                },
                                status = {
                                    StatusView(
                                        projectScope = projectScope,
                                        handleError = Throwable::printStackTrace,
                                        navigateToPath = { path ->
                                            explorerFeature.execute(ExplorerCommand.OpenPath(path = path))
                                        })
                                })
                        })
                })
        }
    }
}