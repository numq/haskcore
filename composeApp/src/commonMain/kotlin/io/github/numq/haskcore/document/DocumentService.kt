package io.github.numq.haskcore.document

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal interface DocumentService {
    suspend fun createDocument(path: String, isReadOnly: Boolean): Result<Document>

    suspend fun editDocument(document: Document.Writable, content: String): Result<Unit>

    suspend fun saveDocument(document: Document.Writable): Result<Unit>

    class Default : DocumentService {
        private fun getSavedContent(filePath: String) = runCatching {
            val path = Path.of(filePath)

            when {
                Files.exists(path) -> Files.readString(path)

                else -> null
            }
        }.getOrNull()

        private fun generateDocumentId(path: String): String {
            val absolutePath = Path.of(path).toAbsolutePath().toString()

            return "editor_document_${absolutePath.hashCode().toString().replace("-", "n")}"
        }

        override suspend fun createDocument(path: String, isReadOnly: Boolean) = runCatching {
            val filePath = Path.of(path)

            val fileName = filePath.fileName.toString()

            if (!Files.exists(filePath)) {
                throw DocumentException("File does not exist: $path")
            }

            val content = Files.readString(filePath)

            when {
                isReadOnly -> Document.ReadOnly(
                    id = generateDocumentId(path), path = path, name = fileName, content = content
                )

                else -> Document.Writable(
                    id = generateDocumentId(path), path = path, name = fileName, content = content
                )
            }
        }

        override suspend fun editDocument(document: Document.Writable, content: String) = runCatching {
            document.copy(content = content, isModified = content != getSavedContent(document.path))

            Unit
        }

        override suspend fun saveDocument(document: Document.Writable) = runCatching {
            Files.writeString(Path.of(document.path), document.content, StandardOpenOption.TRUNCATE_EXISTING)

            Unit
        }
    }
}