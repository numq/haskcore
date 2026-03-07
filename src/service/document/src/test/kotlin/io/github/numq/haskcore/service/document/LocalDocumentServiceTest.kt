package io.github.numq.haskcore.service.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `getName should return document name`() = runTest(UnconfinedTestDispatcher()) {
        val fileName = "test.hs"
        val file = File(tempDir.toFile(), fileName)

        val result = service.getName(file.path)

        assertTrue(result.isRight())
        result.onRight { name ->
            assertEquals(fileName, name)
        }
    }

    @Test
    fun `readDocument should return document content`() = runTest(UnconfinedTestDispatcher()) {
        val fileName = "test.hs"
        val content = "main = putStrLn \"Hello\""
        val file = File(tempDir.toFile(), fileName).apply {
            writeText(content)
        }

        val result = service.readDocument(file.path)

        assertTrue(result.isRight())
        result.onRight { document ->
            assertEquals(file.path, document.path)
            assertEquals(fileName, document.name)
            assertEquals(content, document.content)
            assertTrue(!document.isModified)
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

        assertTrue(result.isRight())
        assertEquals(newContent, file.readText())
    }

    @Test
    fun `readDocument should return error if path is directory`() = runTest(UnconfinedTestDispatcher()) {
        val dirPath = tempDir.toAbsolutePath().toString()

        val result = service.readDocument(dirPath)

        assertTrue(result.isLeft())
        result.onLeft { throwable ->
            assertTrue(throwable is IllegalStateException)
            assertTrue(throwable.message?.contains("is not a file") == true)
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
            assertTrue(result.isLeft())
            result.onLeft { throwable ->
                assertTrue(throwable is IllegalStateException)
                assertTrue(throwable.message?.contains("Cannot write") == true)
            }
        }
    }
}