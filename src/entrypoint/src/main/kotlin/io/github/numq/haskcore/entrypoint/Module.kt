package io.github.numq.haskcore.entrypoint

import io.github.numq.haskcore.service.clipboard.clipboardServiceModule
import io.github.numq.haskcore.service.configuration.configurationServiceModule
import io.github.numq.haskcore.service.document.documentServiceModule
import io.github.numq.haskcore.service.journal.journalServiceModule
import io.github.numq.haskcore.service.keymap.keymapServiceModule
import io.github.numq.haskcore.service.logger.loggerServiceModule
import io.github.numq.haskcore.service.lsp.lspServiceModule
import io.github.numq.haskcore.service.project.projectServiceModule
import io.github.numq.haskcore.service.runtime.runtimeServiceModule
import io.github.numq.haskcore.service.session.sessionServiceModule
import io.github.numq.haskcore.service.syntax.syntaxServiceModule
import io.github.numq.haskcore.service.text.textServiceModule
import io.github.numq.haskcore.service.toolchain.toolchainModule
import io.github.numq.haskcore.service.vfs.vfsServiceModule
import io.github.numq.haskcore.common.presentation.commonPresentationModule
import io.github.numq.haskcore.feature.bootstrap.core.bootstrapFeatureCoreModule
import io.github.numq.haskcore.feature.bootstrap.presentation.bootstrapFeaturePresentationModule
import io.github.numq.haskcore.feature.editor.core.editorFeatureCoreModule
import io.github.numq.haskcore.feature.editor.presentation.editorFeaturePresentationModule
import io.github.numq.haskcore.feature.execution.core.executionFeatureCoreModule
import io.github.numq.haskcore.feature.execution.presentation.executionFeaturePresentationModule
import io.github.numq.haskcore.feature.explorer.core.explorerFeatureCoreModule
import io.github.numq.haskcore.feature.explorer.presentation.explorerFeaturePresentationModule
import io.github.numq.haskcore.feature.log.core.logFeatureCoreModule
import io.github.numq.haskcore.feature.log.presentation.logFeaturePresentationModule
import io.github.numq.haskcore.feature.navigation.core.navigationFeatureCoreModule
import io.github.numq.haskcore.feature.navigation.presentation.navigationFeaturePresentationModule
import io.github.numq.haskcore.feature.output.core.outputFeatureCoreModule
import io.github.numq.haskcore.feature.output.presentation.outputFeaturePresentationModule
import io.github.numq.haskcore.feature.settings.core.settingsFeatureCoreModule
import io.github.numq.haskcore.feature.settings.presentation.settingsFeaturePresentationModule
import io.github.numq.haskcore.feature.status.core.statusFeatureCoreModule
import io.github.numq.haskcore.feature.status.presentation.statusFeaturePresentationModule
import io.github.numq.haskcore.feature.welcome.core.welcomeFeatureCoreModule
import io.github.numq.haskcore.feature.welcome.presentation.welcomeFeaturePresentationModule
import io.github.numq.haskcore.feature.workspace.core.workspaceFeatureCoreModule
import io.github.numq.haskcore.feature.workspace.presentation.workspaceFeaturePresentationModule
import org.koin.dsl.module

private val common = module {
    includes(commonPresentationModule)
}

private val feature = module {
    includes(bootstrapFeatureCoreModule)
    includes(bootstrapFeaturePresentationModule)
    includes(editorFeatureCoreModule)
    includes(editorFeaturePresentationModule)
    includes(executionFeatureCoreModule)
    includes(executionFeaturePresentationModule)
    includes(explorerFeatureCoreModule)
    includes(explorerFeaturePresentationModule)
    includes(logFeatureCoreModule)
    includes(logFeaturePresentationModule)
    includes(navigationFeatureCoreModule)
    includes(navigationFeaturePresentationModule)
    includes(outputFeatureCoreModule)
    includes(outputFeaturePresentationModule)
    includes(settingsFeatureCoreModule)
    includes(settingsFeaturePresentationModule)
    includes(statusFeatureCoreModule)
    includes(statusFeaturePresentationModule)
    includes(welcomeFeatureCoreModule)
    includes(welcomeFeaturePresentationModule)
    includes(workspaceFeatureCoreModule)
    includes(workspaceFeaturePresentationModule)
}

private val service = module {
    includes(clipboardServiceModule)
    includes(configurationServiceModule)
    includes(documentServiceModule)
    includes(syntaxServiceModule)
    includes(journalServiceModule)
    includes(keymapServiceModule)
    includes(lspServiceModule)
    includes(loggerServiceModule)
    includes(projectServiceModule)
    includes(runtimeServiceModule)
    includes(sessionServiceModule)
    includes(textServiceModule)
    includes(toolchainModule)
    includes(vfsServiceModule)
}

val applicationModule = common + feature + service