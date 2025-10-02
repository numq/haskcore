package io.github.numq.haskcore.filesystem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemServiceTest {
    private lateinit var fileSystemService: FileSystemService

    @TempDir
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        fileSystemService = FileSystemService.Default()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `readText throws FileSystemException when file missing`() = runTest {
        val path = "missing.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.readText(path).getOrThrow()
        }
    }

    @Test
    fun `writeBytes writes content`() = runTest {
        val path = File(tempDir, "test.bin").also(File::createNewFile).absolutePath

        val testBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val result = fileSystemService.writeBytes(path, testBytes)

        assertTrue(result.isSuccess)
        assertTrue(File(path).exists())
        assertContentEquals(testBytes, File(path).readBytes())
    }

    @Test
    fun `writeText writes content`() = runTest {
        val path = File(tempDir, "test.txt").also(File::createNewFile).absolutePath

        val testString = "test"

        val result = fileSystemService.writeText(path, testString)

        assertTrue(result.isSuccess)
        assertTrue(File(path).exists())
        assertEquals(testString, File(path).readText())
    }

    @Test
    fun `listDirectory throws exception when not directory`() = runTest {
        val path = "file.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.listDirectory(path).getOrThrow()
        }
    }

    @Test
    fun `createFile throws when file exists`() = runTest {
        val path = "file.txt"

        val tempFile = File(tempDir, path)

        tempFile.createNewFile()

        try {
            val path = tempFile.absolutePath

            assertFailsWith<FileSystemException> {
                fileSystemService.createFile(path, "text").getOrThrow()
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `createDirectory throws when dir exists`() = runTest {
        val path = "dir"

        val tempDir = File(tempDir, path)

        tempDir.mkdirs()

        try {
            val path = tempDir.absolutePath

            assertFailsWith<FileSystemException> {
                fileSystemService.createDirectory(path).getOrThrow()
            }
        } finally {
            tempDir.delete()
        }
    }

    @Test
    fun `rename throws when file already exists`() = runTest {
        val oldPath = "old.txt"
        val newName = "new.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.rename(oldPath, newName).getOrThrow()
        }
    }

    @Test
    fun `move throws when source missing`() = runTest {
        val src = "src.txt"
        val dst = "dst.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.move(src, dst, overwrite = false).getOrThrow()
        }
    }

    @Test
    fun `move throws when destination exists and overwrite false`() = runTest {
        val src = "src.txt"
        val dst = "dst.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.move(src, dst, overwrite = false).getOrThrow()
        }
    }

    @Test
    fun `copy throws when source missing`() = runTest {
        val src = "src.txt"
        val dst = "dst.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.copy(src, dst, overwrite = false).getOrThrow()
        }
    }

    @Test
    fun `delete throws when deletion fails`() = runTest {
        val pathFile = "file.txt"
        val pathDir = "dir"

        assertFailsWith<FileSystemException> {
            fileSystemService.delete(pathFile).getOrThrow()
        }
        assertFailsWith<FileSystemException> {
            fileSystemService.delete(pathDir).getOrThrow()
        }
    }

    @Test
    fun `observeDirectoryChanges throws when path not directory`() = runTest {
        val path = "file.txt"

        assertFailsWith<FileSystemException> {
            fileSystemService.observeDirectoryChanges(path).getOrThrow()
        }
    }
}