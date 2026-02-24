package io.github.numq.haskcore.service.document

import io.github.numq.haskcore.core.di.ScopeQualifier
import io.github.numq.haskcore.core.di.scopedOwner
import org.koin.dsl.bind
import org.koin.dsl.module

val documentModule = module {
    scope<ScopeQualifier.Application> {
        scopedOwner { LocalDocumentService() } bind DocumentService::class
    }
}