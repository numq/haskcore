package io.github.numq.haskcore.service.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalDocumentServiceTest {
    private val service = LocalDocumentService()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `readDocument should return document content`() = runTest(UnconfinedTestDispatcher()) {
        val fileName = "test.hs"
        val content = "main = putStrLn \"Hello\""
        val file = File(tempDir.toFile(), fileName).apply {
            writeText(content)
        }

        val result = service.readDocument(file.path)

        Assertions.assertTrue(result.isRight())
        result.onRight { document ->
            Assertions.assertEquals(file.path, document.path)
            Assertions.assertEquals(fileName, document.name)
            Assertions.assertEquals(content, document.content)
            Assertions.assertTrue(!document.isModified)
        }
    }

    @Test
    fun `saveDocument should write content to file`() = runTest(UnconfinedTestDispatcher()) {
        val fileName = "save_test.hs"
        val newContent = "new content"
        val file = File(tempDir.toFile(), fileName).apply {
            createNewFile()
        }

        val result = service.saveDocument(file.path, newContent)

        Assertions.assertTrue(result.isRight())
        Assertions.assertEquals(newContent, file.readText())
    }

    @Test
    fun `readDocument should return error if path is directory`() = runTest(UnconfinedTestDispatcher()) {
        val dirPath = tempDir.toAbsolutePath().toString()

        val result = service.readDocument(dirPath)

        Assertions.assertTrue(result.isLeft())
        result.onLeft { throwable ->
            Assertions.assertTrue(throwable is IllegalStateException)
            Assertions.assertTrue(throwable.message?.contains("is not a file") == true)
        }
    }

    @Test
    fun `saveDocument should return error if file is not writable`() = runTest(UnconfinedTestDispatcher()) {
        val file = File(tempDir.toFile(), "readonly.hs").apply {
            createNewFile()
            setReadOnly()
        }

        val result = service.saveDocument(file.path, "some content")

        if (!file.canWrite()) {
            Assertions.assertTrue(result.isLeft())
            result.onLeft { throwable ->
                Assertions.assertTrue(throwable is IllegalStateException)
                Assertions.assertTrue(throwable.message?.contains("Cannot write") == true)
            }
        }
    }
}