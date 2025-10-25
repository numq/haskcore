package io.github.numq.haskcore.document

internal interface DocumentRepository {
    suspend fun createDocument(path: String, isReadOnly: Boolean): Result<Document>

    suspend fun editDocument(document: Document.Writable, content: String): Result<Unit>

    suspend fun saveDocument(document: Document.Writable): Result<Unit>

    class Default(
        private val documentService: DocumentService,
    ) : DocumentRepository {
        override suspend fun createDocument(path: String, isReadOnly: Boolean) =
            documentService.createDocument(path = path, isReadOnly = isReadOnly)

        override suspend fun editDocument(document: Document.Writable, content: String) =
            documentService.editDocument(document = document, content = content)

        override suspend fun saveDocument(document: Document.Writable) =
            documentService.saveDocument(document = document)
    }
}