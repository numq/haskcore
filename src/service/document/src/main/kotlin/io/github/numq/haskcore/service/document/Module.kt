package io.github.numq.haskcore.service.document

import io.github.numq.haskcore.common.core.di.ScopeQualifier
import io.github.numq.haskcore.common.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val documentServiceModule = module {
    scope<ScopeQualifier.Type.Application> {
        scopedOwner { LocalDocumentService() } bind DocumentService::class
    }
}