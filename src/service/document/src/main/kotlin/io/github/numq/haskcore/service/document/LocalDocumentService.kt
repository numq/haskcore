package io.github.numq.haskcore.service.document

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

internal class LocalDocumentService : DocumentService {
    private fun getFile(path: String, checkWriteable: Boolean = false): File {
        val file = Path.of(path).toFile()

        check(file.isFile) { "$path is not a file" }

        if (checkWriteable) {
            check(file.canWrite()) { "Cannot write to $path" }
        }

        return file
    }

    private suspend fun readEncodingFromFile(file: File) = withContext(Dispatchers.IO) {
        when {
            file.length() < 2 -> TextEncoding.UTF8

            else -> {
                val bytes = ByteArray(4)

                FileInputStream(file).use { stream ->
                    val bytesRead = stream.read(bytes)

                    val actualBytes = when {
                        bytesRead < 4 -> bytes.copyOf(bytesRead)

                        else -> bytes
                    }

                    TextEncoding.detectFromBOM(bytes = actualBytes)
                }
            }
        }
    }

    private fun writeContent(file: File, content: String, encoding: TextEncoding) {
        var bytes = content.toByteArray(encoding.charset)

        bytes = when (encoding) {
            TextEncoding.UTF16LE -> byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + bytes

            TextEncoding.UTF16BE -> byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + bytes

            else -> bytes
        }

        Files.write(file.toPath(), bytes)
    }

    private fun createMetadata(
        file: File, encoding: TextEncoding,
    ) = Metadata(path = file.path, name = file.name, extension = file.extension, encoding = encoding)

    override suspend fun readMetadata(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val file = getFile(path = path)

            val encoding = readEncodingFromFile(file = file)

            createMetadata(file = file, encoding = encoding)
        }
    }

    override suspend fun readDocument(path: String) = Either.catch {
        withContext(Dispatchers.IO) {
            val file = getFile(path = path)

            val bytes = Files.readAllBytes(file.toPath())

            val encoding = TextEncoding.detectFromBOM(bytes = bytes)

            val content = String(bytes, encoding.bomSize, bytes.size - encoding.bomSize, encoding.charset)

            Document(metadata = createMetadata(file = file, encoding = encoding), content = content, isModified = false)
        }
    }

    override suspend fun saveDocument(path: String, content: String, encoding: TextEncoding) = Either.catch {
        withContext(NonCancellable + Dispatchers.IO) {
            val file = getFile(path = path, checkWriteable = true)

            writeContent(file = file, content = content, encoding = encoding)
        }
    }
}