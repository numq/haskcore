package io.github.numq.haskcore.service.document

import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

internal class LocalDocumentService : DocumentService {
    override suspend fun getParentPath(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).parent.toString()
        }
    }

    override suspend fun getName(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Path.of(path).name
        }
    }

    override suspend fun getLastModifiedTimestamp(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            Timestamp(nanoseconds = Path.of(path).getLastModifiedTime().to(TimeUnit.NANOSECONDS))
        }
    }

    override suspend fun readDocument(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val file = Path.of(path).toFile()

            check(file.isFile) { "$path is not a file" }

            Document(
                path = file.path,
                name = file.name,
                extension = file.extension,
                content = file.readText(),
                isModified = false
            )
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