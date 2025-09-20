package io.github.numq.haskcore.filesystem

import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FileSystemServiceTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var virtualFileSystem: VirtualFileSystem
    private lateinit var fileSystemService: FileSystemService.Default

    @BeforeEach
    fun setUp() {
        virtualFileSystem = mockk(relaxed = true)
        fileSystemService = FileSystemService.Default(virtualFileSystem)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `exists should return true when file exists`() {
        val path = "/test/file.txt"
        every { virtualFileSystem.getNode(path) } returns mockk<FileSystemNode.File>()

        val result = fileSystemService.exists(path)

        Assertions.assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
    }

    @Test
    fun `exists should return false when file does not exist`() {
        val path = "/test/file.txt"
        every { virtualFileSystem.getNode(path) } throws IOException("File not found")

        val result = fileSystemService.exists(path)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun `isFile should return true for file node`() {
        val path = "/test/file.txt"
        every { virtualFileSystem.getNode(path) } returns mockk<FileSystemNode.File>()

        val result = fileSystemService.isFile(path)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
    }

    @Test
    fun `isFile should return false for directory node`() {
        val path = "/test/dir"
        every { virtualFileSystem.getNode(path) } returns mockk<FileSystemNode.Directory>()

        val result = fileSystemService.isFile(path)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun `isDirectory should return true for directory node`() {
        val path = "/test/dir"
        every { virtualFileSystem.getNode(path) } returns mockk<FileSystemNode.Directory>()

        val result = fileSystemService.isDirectory(path)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
    }

    @Test
    fun `listDirectory should return children for non-recursive listing`() {
        val path = "/test/dir"
        val children = listOf(mockk<FileSystemNode.File>(), mockk<FileSystemNode.Directory>())
        val directory = mockk<FileSystemNode.Directory> {
            every { this@mockk.children } returns children
        }
        every { virtualFileSystem.getNode(path) } returns directory

        val result = fileSystemService.listDirectory(path, recursive = false)

        assertTrue(result.isSuccess)
        assertEquals(children, result.getOrThrow())
    }

    @Test
    fun `getNodes should return flattened list of all nodes`() {
        val path = "/test/dir"
        val subFile = mockk<FileSystemNode.File>()
        val subDirectory = mockk<FileSystemNode.Directory> {
            every { children } returns listOf(subFile)
        }
        val children = listOf(mockk<FileSystemNode.File>(), subDirectory)
        val directory = mockk<FileSystemNode.Directory> {
            every { this@mockk.children } returns children
        }
        every { virtualFileSystem.getNode(path) } returns directory

        val result = fileSystemService.getNodes(path, recursive = true)

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrThrow().size)
    }

    @Test
    fun `listDirectory should return flattened list of the root node's children`() {
        val path = "/test/dir"
        val subFile = mockk<FileSystemNode.File>()
        val subDirectory = mockk<FileSystemNode.Directory> {
            every { children } returns listOf(subFile)
        }
        val children = listOf(mockk<FileSystemNode.File>(), subDirectory)
        val directory = mockk<FileSystemNode.Directory> {
            every { this@mockk.children } returns children
        }
        every { virtualFileSystem.getNode(path) } returns directory

        val result = fileSystemService.listDirectory(path, recursive = true)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `observeDirectory should return flow with node changes`() = runTest {
        val directoryPath = "/test/dir"
        val filePath = "$directoryPath/newfile.txt"
        val createdEvent = FileSystemEvent.Created(filePath)
        val flowEvents = flowOf(createdEvent)
        val directory = FileSystemNode.Directory(
            name = "dir",
            path = directoryPath,
            size = 0,
            isHidden = false,
            isReadOnly = false,
            permissions = "rwx",
            createdAt = Instant.DISTANT_PAST,
            accessedAt = Instant.DISTANT_PAST,
            modifiedAt = Instant.DISTANT_PAST,
            parent = null,
            children = emptyList()
        )
        val file = FileSystemNode.File(
            path = filePath,
            name = "newfile.txt",
            size = 0,
            isHidden = false,
            isReadOnly = false,
            permissions = "rw-",
            createdAt = Instant.DISTANT_PAST,
            accessedAt = Instant.DISTANT_PAST,
            modifiedAt = Instant.DISTANT_PAST,
            parent = directory
        )
        every { virtualFileSystem.getNode(directoryPath) } returns directory
        every { virtualFileSystem.watch(directoryPath) } returns flowEvents
        every { virtualFileSystem.getNode(filePath) } returns file

        val result = fileSystemService.observeDirectory(directoryPath)
        val events = result.getOrThrow().toList()

        assertTrue(result.isSuccess)
        assertEquals(1, events.size)
    }

    @Test
    fun `createDirectory should invalidate cache and parent cache`() {
        val path = tempDir.resolve("newdir").toString()
        val parentPath = tempDir.toString()

        every { virtualFileSystem.invalidateCache(path) } just Runs
        every { virtualFileSystem.invalidateCache(parentPath) } just Runs

        val result = fileSystemService.createDirectory(path)

        assertTrue(result.isSuccess)
        assertTrue(File(path).exists())
        verify { virtualFileSystem.invalidateCache(path) }
        verify { virtualFileSystem.invalidateCache(parentPath) }
    }

    @Test
    fun `createFile with text should write file and invalidate cache`() {
        val path = tempDir.resolve("file.txt").toString()
        val text = "Hello World"
        val parentPath = tempDir.toString()

        every { virtualFileSystem.invalidateCache(path) } just Runs
        every { virtualFileSystem.invalidateCache(parentPath) } just Runs

        val result = fileSystemService.createFile(path, text)

        assertTrue(result.isSuccess)
        assertEquals(text, File(path).readText())
        verify { virtualFileSystem.invalidateCache(path) }
        verify { virtualFileSystem.invalidateCache(parentPath) }
    }

    @Test
    fun `createFile with bytes should write file and invalidate cache`() {
        val path = tempDir.resolve("file.bin").toString()
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val parentPath = tempDir.toString()

        every { virtualFileSystem.invalidateCache(path) } just Runs
        every { virtualFileSystem.invalidateCache(parentPath) } just Runs

        val result = fileSystemService.createFile(path, bytes)

        assertTrue(result.isSuccess)
        assertContentEquals(bytes, File(path).readBytes())
        verify { virtualFileSystem.invalidateCache(path) }
        verify { virtualFileSystem.invalidateCache(parentPath) }
    }

    @Test
    fun `delete should invalidate cache and parent cache`() {
        val path = tempDir.resolve("file.txt").toString()
        val parentPath = tempDir.toString()

        File(path).writeText("test content")

        every { virtualFileSystem.invalidateCache(path) } just Runs
        every { virtualFileSystem.invalidateCache(parentPath) } just Runs

        val result = fileSystemService.delete(path)

        assertTrue(result.isSuccess)
        assertFalse(File(path).exists())
        verify { virtualFileSystem.invalidateCache(path) }
        verify { virtualFileSystem.invalidateCache(parentPath) }
    }

    @Test
    fun `rename should change file name and invalidate cache`() {
        val originalPath = tempDir.resolve("old.txt").toString()
        val newPath = tempDir.resolve("new.txt").toString()
        val parentPath = tempDir.toString()

        File(originalPath).writeText("test content")

        every { virtualFileSystem.invalidateCache(originalPath) } just Runs
        every { virtualFileSystem.invalidateCache(newPath) } just Runs
        every { virtualFileSystem.invalidateCache(parentPath) } just Runs

        val result = fileSystemService.rename(originalPath, "new.txt")

        assertTrue(result.isSuccess)
        assertFalse(File(originalPath).exists())
        assertTrue(File(newPath).exists())
        verify { virtualFileSystem.invalidateCache(originalPath) }
        verify { virtualFileSystem.invalidateCache(newPath) }
        verify { virtualFileSystem.invalidateCache(parentPath) }
    }

    @Test
    fun `move should move file and invalidate both source and destination caches`() {
        val sourcePath = tempDir.resolve("source.txt").toString()
        val destPath = tempDir.resolve("subdir/dest.txt").toString()
        val sourceParent = tempDir.toString()
        val destParent = tempDir.resolve("subdir").toString()

        File(sourcePath).writeText("test content")
        File(tempDir.resolve("subdir").toString()).mkdirs()

        every { virtualFileSystem.invalidateCache(sourcePath) } just Runs
        every { virtualFileSystem.invalidateCache(destPath) } just Runs
        every { virtualFileSystem.invalidateCache(sourceParent) } just Runs
        every { virtualFileSystem.invalidateCache(destParent) } just Runs

        val result = fileSystemService.move(sourcePath, destPath, overwrite = false)

        assertTrue(result.isSuccess)
        assertFalse(File(sourcePath).exists())
        assertTrue(File(destPath).exists())
        verify { virtualFileSystem.invalidateCache(sourcePath) }
        verify { virtualFileSystem.invalidateCache(destPath) }
        verify { virtualFileSystem.invalidateCache(sourceParent) }
        verify { virtualFileSystem.invalidateCache(destParent) }
    }

    @Test
    fun `copy should copy file and invalidate destination cache`() {
        val sourcePath = tempDir.resolve("source.txt").toString()
        val destPath = tempDir.resolve("copy.txt").toString()
        val destParent = tempDir.toString()

        File(sourcePath).writeText("test content")

        every { virtualFileSystem.invalidateCache(destPath) } just Runs
        every { virtualFileSystem.invalidateCache(destParent) } just Runs

        val result = fileSystemService.copy(sourcePath, destPath, overwrite = false)

        assertTrue(result.isSuccess)
        assertTrue(File(sourcePath).exists())
        assertTrue(File(destPath).exists())
        verify { virtualFileSystem.invalidateCache(destPath) }
        verify { virtualFileSystem.invalidateCache(destParent) }
    }

    @Test
    fun `should not return failure when operation throws IOException`() {
        val path = "/test/file.txt"
        every { virtualFileSystem.getNode(path) } throws IOException("Connection failed")

        val result = fileSystemService.exists(path)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrThrow())
    }

    @Test
    fun `delete should fail when file does not exist`() {
        val nonExistentPath = tempDir.resolve("nonexistent").toString()

        val result = fileSystemService.delete(nonExistentPath)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileSystemException)
    }

    @Test
    fun `rename should fail when source does not exist`() {
        val nonExistentPath = tempDir.resolve("nonexistent").toString()

        val result = fileSystemService.rename(nonExistentPath, "new.txt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileSystemException)
    }
}