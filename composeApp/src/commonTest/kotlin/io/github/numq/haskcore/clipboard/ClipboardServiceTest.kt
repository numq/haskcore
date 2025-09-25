package io.github.numq.haskcore.clipboard

import io.github.numq.haskcore.filesystem.FileSystemService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardServiceTest {
    private lateinit var mockFileSystemService: FileSystemService
    private lateinit var clipboardService: ClipboardService

    @BeforeTest
    fun setUp() {
        mockFileSystemService = mockk()
        clipboardService = ClipboardService.Default(mockFileSystemService)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        runBlocking {
            clipboardService.clearClipboard().getOrThrow()
        }
    }

    @Test
    fun `cut should update clipboard with Cut operation`() = runTest {
        val testPaths = listOf("/path/to/file1", "/path/to/file2")

        val result = clipboardService.cut(testPaths)

        assertTrue(result.isSuccess)
        assertIs<Clipboard.Cut>(clipboardService.clipboard.value)
        assertEquals(testPaths, (clipboardService.clipboard.value as Clipboard.Cut).paths)
    }

    @Test
    fun `copy should update clipboard with Copy operation`() = runTest {
        val testPaths = listOf("/path/to/file1", "/path/to/file2")

        val result = clipboardService.copy(testPaths)

        assertTrue(result.isSuccess)
        assertIs<Clipboard.Copy>(clipboardService.clipboard.value)
        assertEquals(testPaths, (clipboardService.clipboard.value as Clipboard.Copy).paths)
    }

    @Test
    fun `reset should set clipboard to Empty`() = runTest {
        clipboardService.cut(listOf("/test/path"))

        val result = clipboardService.clearClipboard()

        assertTrue(result.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)
    }

    @Test
    fun `paste with Empty clipboard should do nothing`() = runTest {
        val result = clipboardService.paste("/target/path")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) {
            mockFileSystemService.move(any(), any(), any())
            mockFileSystemService.copy(any(), any(), any())
        }
    }

    @Test
    fun `paste with Cut clipboard should move files and reset clipboard`() = runTest {
        val sourcePaths = listOf("/source/file1.txt", "/source/file2.txt")
        val targetPath = "/target/dir"

        coEvery {
            mockFileSystemService.move(
                eq("/source/file1.txt"),
                eq("/target/dir/file1.txt"),
                eq(false)
            )
        } returns Result.success(Unit)

        coEvery {
            mockFileSystemService.move(
                eq("/source/file2.txt"),
                eq("/target/dir/file2.txt"),
                eq(false)
            )
        } returns Result.success(Unit)

        clipboardService.cut(sourcePaths).getOrThrow()

        val result = clipboardService.paste(targetPath)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) {
            mockFileSystemService.move(
                fromPath = "/source/file1.txt",
                toPath = "/target/dir/file1.txt",
                overwrite = false
            )
        }
        coVerify(exactly = 1) {
            mockFileSystemService.move(
                fromPath = "/source/file2.txt",
                toPath = "/target/dir/file2.txt",
                overwrite = false
            )
        }

        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)
    }

    @Test
    fun `paste with Copy clipboard should copy files and keep clipboard unchanged`() = runTest {
        val sourcePaths = listOf("/source/file1.txt", "/source/file2.txt")
        val targetPath = "/target/dir"

        coEvery {
            mockFileSystemService.copy(
                eq("/source/file1.txt"),
                eq("/target/dir/file1.txt"),
                eq(false)
            )
        } returns Result.success(Unit)

        coEvery {
            mockFileSystemService.copy(
                eq("/source/file2.txt"),
                eq("/target/dir/file2.txt"),
                eq(false)
            )
        } returns Result.success(Unit)

        clipboardService.copy(sourcePaths).getOrThrow()

        val result = clipboardService.paste(targetPath)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) {
            mockFileSystemService.copy(
                fromPath = "/source/file1.txt",
                toPath = "/target/dir/file1.txt",
                overwrite = false
            )
        }
        coVerify(exactly = 1) {
            mockFileSystemService.copy(
                fromPath = "/source/file2.txt",
                toPath = "/target/dir/file2.txt",
                overwrite = false
            )
        }

        assertIs<Clipboard.Copy>(clipboardService.clipboard.value)
        assertEquals(sourcePaths, (clipboardService.clipboard.value as Clipboard.Copy).paths)
    }

    @Test
    fun `paste with Cut clipboard should return failure when move fails`() = runTest {
        val sourcePaths = listOf("/source/file1.txt")
        val targetPath = "/target/dir"
        val error = RuntimeException("Move failed")

        coEvery {
            mockFileSystemService.move(
                eq("/source/file1.txt"),
                eq("/target/dir/file1.txt"),
                eq(false)
            )
        } returns Result.failure(error)

        clipboardService.cut(sourcePaths).getOrThrow()

        val result = clipboardService.paste(targetPath)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        assertIs<Clipboard.Cut>(clipboardService.clipboard.value)
        assertEquals(sourcePaths, (clipboardService.clipboard.value as Clipboard.Cut).paths)
    }

    @Test
    fun `paste with Copy clipboard should return failure when copy fails`() = runTest {
        val sourcePaths = listOf("/source/file1.txt")
        val targetPath = "/target/dir"
        val error = RuntimeException("Copy failed")

        coEvery {
            mockFileSystemService.copy(
                eq("/source/file1.txt"),
                eq("/target/dir/file1.txt"),
                eq(false)
            )
        } returns Result.failure(error)

        clipboardService.copy(sourcePaths).getOrThrow()

        val result = clipboardService.paste(targetPath)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        assertIs<Clipboard.Copy>(clipboardService.clipboard.value)
        assertEquals(sourcePaths, (clipboardService.clipboard.value as Clipboard.Copy).paths)
    }

    @Test
    fun `paste with Cut clipboard should handle first move failure and stop processing`() = runTest {
        val sourcePaths = listOf("/source/file1.txt", "/source/file2.txt")
        val targetPath = "/target/dir"
        val error = RuntimeException("Move failed")

        coEvery {
            mockFileSystemService.move(
                eq("/source/file1.txt"),
                eq("/target/dir/file1.txt"),
                eq(false)
            )
        } returns Result.failure(error)

        clipboardService.cut(sourcePaths).getOrThrow()

        val result = clipboardService.paste(targetPath)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())

        coVerify(exactly = 1) {
            mockFileSystemService.move(any(), any(), any())
        }

        assertIs<Clipboard.Cut>(clipboardService.clipboard.value)
        assertEquals(sourcePaths, (clipboardService.clipboard.value as Clipboard.Cut).paths)
    }

    @Test
    fun `removeFromClipboard should remove single path from Cut clipboard`() = runTest {
        val testPaths = listOf("/file1", "/file2")
        clipboardService.cut(testPaths).getOrThrow()

        val result = clipboardService.removeFromClipboard("/file1")

        assertTrue(result.isSuccess)
        val clipboard = clipboardService.clipboard.value
        assertIs<Clipboard.Cut>(clipboard)
        assertEquals(listOf("/file2"), clipboard.paths)
    }

    @Test
    fun `removeFromClipboard should empty Cut clipboard when last path removed`() = runTest {
        val testPaths = listOf("/file1")
        clipboardService.cut(testPaths).getOrThrow()

        val result = clipboardService.removeFromClipboard("/file1")

        assertTrue(result.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)
    }

    @Test
    fun `removeFromClipboard should remove single path from Copy clipboard`() = runTest {
        val testPaths = listOf("/file1", "/file2")
        clipboardService.copy(testPaths).getOrThrow()

        val result = clipboardService.removeFromClipboard("/file2")

        assertTrue(result.isSuccess)
        val clipboard = clipboardService.clipboard.value
        assertIs<Clipboard.Copy>(clipboard)
        assertEquals(listOf("/file1"), clipboard.paths)
    }

    @Test
    fun `removeFromClipboard should empty Copy clipboard when last path removed`() = runTest {
        val testPaths = listOf("/file1")
        clipboardService.copy(testPaths).getOrThrow()

        val result = clipboardService.removeFromClipboard("/file1")

        assertTrue(result.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)
    }

    @Test
    fun `clearClipboard should empty clipboard regardless of type`() = runTest {
        clipboardService.cut(listOf("/file1")).getOrThrow()
        val cutResult = clipboardService.clearClipboard()
        assertTrue(cutResult.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)

        clipboardService.copy(listOf("/file2")).getOrThrow()
        val copyResult = clipboardService.clearClipboard()
        assertTrue(copyResult.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)

        val emptyResult = clipboardService.clearClipboard()
        assertTrue(emptyResult.isSuccess)
        assertIs<Clipboard.Empty>(clipboardService.clipboard.value)
    }
}