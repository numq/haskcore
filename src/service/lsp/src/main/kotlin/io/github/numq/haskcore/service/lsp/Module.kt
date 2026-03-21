package io.github.numq.haskcore.service.lsp

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.ScopeQualifierType
import io.github.numq.haskcore.core.di.scopedOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val lspModule = module {
    scope<ScopeQualifierType.Project> {
        scopedOwner {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val projectPath = get<String>(qualifier = ScopeQualifier.Project)

            HaskellLspService(projectPath = projectPath, scope = scope)
        } bind LspService::class
    }
}