package io.github.numq.haskcore.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.application
import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.feature.Feature
import io.github.numq.haskcore.feature.bootstrap.core.bootstrapCoreModule
import io.github.numq.haskcore.feature.bootstrap.presentation.bootstrapPresentationModule
import io.github.numq.haskcore.feature.bootstrap.presentation.feature.BootstrapView
import io.github.numq.haskcore.feature.editor.core.editorCoreModule
import io.github.numq.haskcore.feature.editor.presentation.editorPresentationModule
import io.github.numq.haskcore.feature.editor.presentation.feature.view.EditorView
import io.github.numq.haskcore.feature.editor.presentation.layer.LayerFactory
import io.github.numq.haskcore.feature.execution.core.executionCoreModule
import io.github.numq.haskcore.feature.execution.presentation.executionPresentationModule
import io.github.numq.haskcore.feature.execution.presentation.feature.ExecutionView
import io.github.numq.haskcore.feature.explorer.core.explorerCoreModule
import io.github.numq.haskcore.feature.explorer.presentation.explorerPresentationModule
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerCommand
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerEvent
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerState
import io.github.numq.haskcore.feature.explorer.presentation.feature.ExplorerView
import io.github.numq.haskcore.feature.log.core.logCoreModule
import io.github.numq.haskcore.feature.log.presentation.feature.LogView
import io.github.numq.haskcore.feature.log.presentation.logPresentationModule
import io.github.numq.haskcore.feature.navigation.core.Destination
import io.github.numq.haskcore.feature.navigation.core.navigationCoreModule
import io.github.numq.haskcore.feature.navigation.presentation.feature.NavigationView
import io.github.numq.haskcore.feature.navigation.presentation.navigationPresentationModule
import io.github.numq.haskcore.feature.output.core.outputCoreModule
import io.github.numq.haskcore.feature.output.presentation.feature.OutputView
import io.github.numq.haskcore.feature.output.presentation.outputPresentationModule
import io.github.numq.haskcore.feature.settings.core.settingsCoreModule
import io.github.numq.haskcore.feature.settings.presentation.settingsPresentationModule
import io.github.numq.haskcore.feature.shelf.core.shelfCoreModule
import io.github.numq.haskcore.feature.shelf.presentation.feature.ShelfView
import io.github.numq.haskcore.feature.shelf.presentation.shelfPresentationModule
import io.github.numq.haskcore.feature.status.core.statusCoreModule
import io.github.numq.haskcore.feature.status.presentation.feature.StatusView
import io.github.numq.haskcore.feature.status.presentation.statusPresentationModule
import io.github.numq.haskcore.feature.welcome.core.welcomeCoreModule
import io.github.numq.haskcore.feature.welcome.presentation.feature.WelcomeView
import io.github.numq.haskcore.feature.welcome.presentation.welcomePresentationModule
import io.github.numq.haskcore.feature.workspace.core.Workspace
import io.github.numq.haskcore.feature.workspace.core.workspaceCoreModule
import io.github.numq.haskcore.feature.workspace.presentation.feature.WorkspaceView
import io.github.numq.haskcore.feature.workspace.presentation.workspacePresentationModule
import io.github.numq.haskcore.platform.font.*
import io.github.numq.haskcore.platform.overlay.overlayModule
import io.github.numq.haskcore.platform.theme.application.ApplicationTheme
import io.github.numq.haskcore.platform.theme.editor.EditorTheme
import io.github.numq.haskcore.platform.theme.themeModule
import io.github.numq.haskcore.service.clipboard.clipboardModule
import io.github.numq.haskcore.service.configuration.configurationModule
import io.github.numq.haskcore.service.document.documentModule
import io.github.numq.haskcore.service.syntax.syntaxModule
import io.github.numq.haskcore.service.journal.journalModule
import io.github.numq.haskcore.service.keymap.keymapModule
import io.github.numq.haskcore.service.lsp.lspModule
import io.github.numq.haskcore.service.logger.loggerModule
import io.github.numq.haskcore.service.project.projectModule
import io.github.numq.haskcore.service.runtime.runtimeModule
import io.github.numq.haskcore.service.session.sessionModule
import io.github.numq.haskcore.service.text.textModule
import io.github.numq.haskcore.service.toolchain.toolchainModule
import io.github.numq.haskcore.service.vfs.vfsModule
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private const val APPLICATION_NAME = "haskcore"

private const val USER_HOME = "user.home"

internal fun main() {
    System.setProperty("skiko.renderApi", "OPENGL")

    startKoin {
        modules(
            bootstrapCoreModule,
            bootstrapPresentationModule,
            editorCoreModule,
            editorPresentationModule,
            executionCoreModule,
            executionPresentationModule,
            explorerCoreModule,
            explorerPresentationModule,
            logCoreModule,
            logPresentationModule,
            navigationCoreModule,
            navigationPresentationModule,
            outputCoreModule,
            outputPresentationModule,
            settingsCoreModule,
            settingsPresentationModule,
            shelfCoreModule,
            shelfPresentationModule,
            statusCoreModule,
            statusPresentationModule,
            welcomeCoreModule,
            welcomePresentationModule,
            workspaceCoreModule,
            workspacePresentationModule
        )

        modules(fontModule, overlayModule, themeModule)

        modules(
            clipboardModule,
            configurationModule,
            documentModule,
            syntaxModule,
            journalModule,
            keymapModule,
            lspModule,
            loggerModule,
            projectModule,
            runtimeModule,
            sessionModule,
            textModule,
            toolchainModule,
            vfsModule
        )
    }

    val applicationPath = Path.of(System.getProperty(USER_HOME)).absolutePathString()

    application {
        val koin = getKoin()

        val applicationScope = remember(applicationPath) {
            koin.getOrCreateScope(
                scopeId = applicationPath, qualifier = named<ScopeQualifierType.Application>(), source = applicationPath
            ).apply {
                declare(instance = applicationPath, qualifier = ScopeQualifier.Application)
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

        val icon = painterResource("drawable/icon.png")

        val isDark = isSystemInDarkTheme()

        val editorTheme = koinInject<EditorTheme>(scope = applicationScope) { parametersOf(isDark) }

        val layerFactory = koinInject<LayerFactory>(scope = applicationScope)

        ApplicationTheme(isDark = isDark) {
            BootstrapView(
                applicationScope = applicationScope,
                handleError = Throwable::printStackTrace,
                title = APPLICATION_NAME,
                icon = icon,
                exitApplication = ::exitApplication,
                content = { bootstrap ->
                    NavigationView(
                        applicationScope = applicationScope,
                        handleError = Throwable::printStackTrace,
                        initialDestinations = bootstrap.items.map { item ->
                            Destination(path = item.path, initialWorkspace = null)
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
                                exitApplication = ::exitApplication
                            )
                        },
                        workspace = { destination ->
                            val projectScope = remember(applicationScope.id) {
                                val projectPath = destination.path

                                koin.getOrCreateScope(
                                    scopeId = projectPath,
                                    qualifier = named<ScopeQualifierType.Project>(),
                                    source = projectPath
                                ).apply {
                                    linkTo(applicationScope)

                                    declare(instance = projectPath, qualifier = ScopeQualifier.Project)
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
                                workspace = destination.initialWorkspace?.run {
                                    Workspace(
                                        x = x,
                                        y = y,
                                        width = width,
                                        height = height,
                                        ratio = ratio ?: Workspace.DEFAULT_RATIO
                                    )
                                } ?: Workspace(),
                                icon = icon,
                                execution = {
                                    ExecutionView(projectScope = projectScope, handleError = Throwable::printStackTrace)
                                },
                                editor = { path, tabs ->
                                    ShelfView(
                                        projectScope = projectScope,
                                        handleError = Throwable::printStackTrace,
                                        explorer = {
                                            ExplorerView(
                                                feature = explorerFeature,
                                                handleError = Throwable::printStackTrace,
                                                selectedPath = path
                                            )
                                        },
                                        log = {
                                            LogView(
                                                projectScope = projectScope, handleError = Throwable::printStackTrace
                                            )
                                        },
                                        editor = {
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
                                        })
                                },
                                output = {
                                    OutputView(
                                        projectScope = projectScope, handleError = Throwable::printStackTrace
                                    )
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