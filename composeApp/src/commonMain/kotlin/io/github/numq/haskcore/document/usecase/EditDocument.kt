package io.github.numq.haskcore.document.usecase

import io.github.numq.haskcore.document.Document
import io.github.numq.haskcore.document.DocumentRepository
import io.github.numq.haskcore.usecase.UseCase

internal class EditDocument(
    private val documentRepository: DocumentRepository
) : UseCase<EditDocument.Input, Unit> {
    data class Input(val document: Document.Writable, val content: String)

    override suspend fun execute(input: Input) = documentRepository.editDocument(
        document = input.document, content = input.content
    )
}