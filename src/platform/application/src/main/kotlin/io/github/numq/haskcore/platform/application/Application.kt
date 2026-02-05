package io.github.numq.haskcore.platform.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import io.github.numq.haskcore.core.di.Dependency.getOrCreateScope
import io.github.numq.haskcore.core.di.ScopeContext
import io.github.numq.haskcore.feature.editor.core.editorCoreModule
import io.github.numq.haskcore.feature.editor.presentation.editorPresentationModule
import io.github.numq.haskcore.feature.explorer.core.explorerCoreModule
import io.github.numq.haskcore.feature.explorer.presentation.explorerPresentationModule
import io.github.numq.haskcore.feature.output.core.outputCoreModule
import io.github.numq.haskcore.feature.output.presentation.outputPresentationModule
import io.github.numq.haskcore.feature.settings.core.settingsCoreModule
import io.github.numq.haskcore.feature.settings.presentation.settingsPresentationModule
import io.github.numq.haskcore.feature.workspace.core.workspaceCoreModule
import io.github.numq.haskcore.feature.workspace.presentation.workspacePresentationModule
import io.github.numq.haskcore.platform.navigation.NavigationView
import io.github.numq.haskcore.platform.navigation.navigationModule
import io.github.numq.haskcore.platform.ui.theme.Theme
import io.github.numq.haskcore.service.configuration.configurationModule
import io.github.numq.haskcore.service.document.documentModule
import io.github.numq.haskcore.service.language.languageModule
import io.github.numq.haskcore.service.project.projectModule
import io.github.numq.haskcore.service.runtime.runtimeModule
import io.github.numq.haskcore.service.session.sessionModule
import io.github.numq.haskcore.service.toolchain.toolchainModule
import io.github.numq.haskcore.service.vfs.vfsModule
import org.koin.compose.getKoin
import org.koin.core.context.GlobalContext.startKoin

private const val APP_NAME = "haskcore"

internal fun main() {
    startKoin {
        val platformModules = listOf(navigationModule)

        val serviceModules = listOf(
            configurationModule,
            documentModule,
            languageModule,
            projectModule,
            runtimeModule,
            sessionModule,
            toolchainModule,
            vfsModule
        )

        val featureModules = listOf(
            editorCoreModule,
            editorPresentationModule,
            explorerCoreModule,
            explorerPresentationModule,
            outputCoreModule,
            outputPresentationModule,
            settingsCoreModule,
            settingsPresentationModule,
            workspaceCoreModule,
            workspacePresentationModule
        )

        modules(platformModules + serviceModules + featureModules)
    }

    application {
        val koin = getKoin()

        val applicationScope = remember(APP_NAME) {
            val context = ScopeContext.Application(name = APP_NAME)

            koin.getOrCreateScope(context = context)
        }

        DisposableEffect(applicationScope.id) {
            onDispose {
                applicationScope.close()
            }
        }

        Theme(isDark = isSystemInDarkTheme()) {
            NavigationView(
                applicationScope = applicationScope,
                title = APP_NAME,
                onCloseRequest = ::exitApplication,
                handleError = { throwable ->
                    throwable.printStackTrace() // todo
                })
        }
    }
}