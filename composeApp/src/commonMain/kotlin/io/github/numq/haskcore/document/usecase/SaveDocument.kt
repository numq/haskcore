package io.github.numq.haskcore.document.usecase

import io.github.numq.haskcore.document.Document
import io.github.numq.haskcore.document.DocumentRepository
import io.github.numq.haskcore.usecase.UseCase

internal class SaveDocument(
    private val documentRepository: DocumentRepository
) : UseCase<SaveDocument.Input, Unit> {
    data class Input(val document: Document.Writable)

    override suspend fun execute(input: Input) = documentRepository.saveDocument(document = input.document)
}