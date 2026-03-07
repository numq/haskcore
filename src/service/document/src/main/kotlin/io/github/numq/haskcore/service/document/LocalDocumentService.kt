package io.github.numq.haskcore.service.document

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.name

internal class LocalDocumentService : DocumentService {
    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

    override suspend fun readDocument(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val file = Path.of(path).toFile()

            check(file.isFile) { "$path is not a file" }

            Document(path = file.path, name = file.name, content = file.readText(), isModified = false)
        }
    }

    override suspend fun saveDocument(path: String, content: String) = Either.catch {
        withContext(NonCancellable + Dispatchers.IO) {
            val file = Path.of(path).toFile()

            check(file.isFile) { "$path is not a file" }

            check(file.canWrite()) { "Cannot write to $path" }

            file.writeText(text = content, charset = Charsets.UTF_8)
        }
    }
}