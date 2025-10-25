package io.github.numq.haskcore.document

import io.github.numq.haskcore.document.usecase.EditDocument
import io.github.numq.haskcore.document.usecase.SaveDocument
import org.koin.dsl.bind
import org.koin.dsl.module

internal val documentModule = module {
    single { DocumentService.Default() } bind DocumentService::class

    single { DocumentRepository.Default(documentService = get()) } bind DocumentRepository::class

    single { EditDocument(documentRepository = get()) }

    single { SaveDocument(documentRepository = get()) }
}