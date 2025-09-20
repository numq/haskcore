package io.github.numq.haskcore.filesystem

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.*

class VirtualFileSystemTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var virtualFileSystem: VirtualFileSystem.Default

    @BeforeEach
    fun setUp() {
        virtualFileSystem = VirtualFileSystem.Default()
    }

    @AfterEach
    fun tearDown() {
        virtualFileSystem.invalidateCache()
    }

    @Test
    fun `getNode should return file node for file`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Hello World")

        val node = virtualFileSystem.getNode(testFile.absolutePath)

        assertTrue(node is FileSystemNode.File)
        assertEquals("test.txt", node.name)
        assertEquals(testFile.absolutePath, node.path)
        assertEquals(11, node.size) // "Hello World" length
        assertFalse(node.isHidden)
    }

    @Test
    fun `getNode should return directory node for directory`() {
        val testDir = tempDir.resolve("testDir").toFile()
        testDir.mkdirs()

        val node = virtualFileSystem.getNode(testDir.absolutePath)

        assertTrue(node is FileSystemNode.Directory)
        assertEquals("testDir", node.name)
        assertEquals(testDir.absolutePath, node.path)
        assertFalse(node.isHidden)
    }

    @Test
    fun `getNode should throw IOException for non-existent path`() {
        val nonExistentPath = tempDir.resolve("nonexistent").absolutePathString()

        assertThrows<IOException> {
            virtualFileSystem.getNode(nonExistentPath)
        }
    }

    @Test
    fun `getNode should use cache for subsequent calls`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Hello World")

        val firstCall = virtualFileSystem.getNode(testFile.absolutePath)
        val secondCall = virtualFileSystem.getNode(testFile.absolutePath)

        assertSame(firstCall, secondCall)
    }

    @Test
    fun `getChildren should return empty list for file`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Hello World")

        val children = virtualFileSystem.getChildren(testFile.absolutePath)

        assertTrue(children.isEmpty())
    }

    @Test
    fun `getChildren should return children for directory`() {
        val testDir = tempDir.resolve("testDir").toFile()
        testDir.mkdirs()

        val file1 = File(testDir, "file1.txt")
        file1.writeText("content1")

        val file2 = File(testDir, "file2.txt")
        file2.writeText("content2")

        val children = virtualFileSystem.getChildren(testDir.absolutePath)

        assertEquals(2, children.size)
        assertEquals(setOf("file1.txt", "file2.txt"), children.map { it.name }.toSet())
    }

    @Test
    fun `getChildren should use cache for subsequent calls`() {
        val testDir = tempDir.resolve("testDir").toFile()
        testDir.mkdirs()

        val testFile = File(testDir, "test.txt")
        testFile.writeText("Hello World")

        val firstCall = virtualFileSystem.getChildren(testDir.absolutePath)
        val secondCall = virtualFileSystem.getChildren(testDir.absolutePath)

        assertSame(firstCall, secondCall)
    }

    @Test
    fun `invalidateCache should clear specific path from cache`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Hello World")

        val cachedNode = virtualFileSystem.getNode(testFile.absolutePath)

        virtualFileSystem.invalidateCache(testFile.absolutePath)
        val newNode = virtualFileSystem.getNode(testFile.absolutePath)

        assertNotSame(cachedNode, newNode)
    }

    @Test
    fun `invalidateCache should clear all cache`() {
        val testFile1 = tempDir.resolve("test1.txt").toFile()
        testFile1.writeText("Hello")

        val testFile2 = tempDir.resolve("test2.txt").toFile()
        testFile2.writeText("World")

        val cachedNode1 = virtualFileSystem.getNode(testFile1.absolutePath)
        val cachedNode2 = virtualFileSystem.getNode(testFile2.absolutePath)

        virtualFileSystem.invalidateCache()
        val newNode1 = virtualFileSystem.getNode(testFile1.absolutePath)
        val newNode2 = virtualFileSystem.getNode(testFile2.absolutePath)

        assertNotSame(cachedNode1, newNode1)
        assertNotSame(cachedNode2, newNode2)
    }

    @Test
    fun `invalidateCache should recursively clear children cache`() {
        val testDir = tempDir.resolve("testDir").toFile()
        testDir.mkdirs()

        val subDir = File(testDir, "subDir")
        subDir.mkdirs()

        val testFile = File(subDir, "test.txt")
        testFile.writeText("Hello World")

        virtualFileSystem.getNode(testDir.absolutePath)
        virtualFileSystem.getNode(subDir.absolutePath)
        virtualFileSystem.getNode(testFile.absolutePath)

        virtualFileSystem.invalidateCache(testDir.absolutePath)

        val newNode = virtualFileSystem.getNode(testDir.absolutePath)
        assertNotNull(newNode)
    }

    @Test
    fun `buildNode should handle file permissions`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Hello World")
        testFile.setReadable(true)
        testFile.setWritable(true)
        testFile.setExecutable(false)

        val node = virtualFileSystem.getNode(testFile.absolutePath)

        assertTrue(node.permissions.contains('r'))
        assertTrue(node.permissions.contains('w'))
        assertEquals(node.isReadOnly, !testFile.canWrite())
    }

    @Test
    fun `buildNode should handle hidden files on unix systems`() {
        val testFile = tempDir.resolve(".hidden").toFile()
        testFile.writeText("hidden content")

        val node = virtualFileSystem.getNode(testFile.absolutePath)

        if (System.getProperty("os.name").contains("win", ignoreCase = true).not()) {
            assertTrue(node.isHidden)
        }
    }

    @Test
    fun `buildNode should calculate directory size correctly`() {
        val testDir = tempDir.resolve("sizeTest").toFile()
        testDir.mkdirs()

        val file1 = File(testDir, "file1.txt")
        file1.writeText("12345")

        val file2 = File(testDir, "file2.txt")
        file2.writeText("67890")

        val node = virtualFileSystem.getNode(testDir.absolutePath) as FileSystemNode.Directory

        assertTrue(node.size >= 10L)
    }

    @Test
    fun `should handle nested directory structure`() {
        val rootDir = tempDir.resolve("root").toFile()
        rootDir.mkdirs()

        val subDir = File(rootDir, "sub")
        subDir.mkdirs()

        val testFile = File(subDir, "test.txt")
        testFile.writeText("content")

        val rootNode = virtualFileSystem.getNode(rootDir.absolutePath) as FileSystemNode.Directory
        val subNode = virtualFileSystem.getNode(subDir.absolutePath) as FileSystemNode.Directory
        val fileNode = virtualFileSystem.getNode(testFile.absolutePath) as FileSystemNode.File

        assertEquals(1, rootNode.children.size)
        assertEquals("sub", rootNode.children[0].name)
        assertEquals(1, subNode.children.size)
        assertEquals("test.txt", subNode.children[0].name)
        assertEquals("content".length.toLong(), fileNode.size)
    }

    @Test
    fun `should handle empty directory`() {
        val emptyDir = tempDir.resolve("empty").toFile()
        emptyDir.mkdirs()

        val node = virtualFileSystem.getNode(emptyDir.absolutePath) as FileSystemNode.Directory

        assertTrue(node.children.isEmpty())
        assertEquals(0L, node.size)
    }

    @Test
    fun `should handle file with special characters in name`() {
        val testFile = tempDir.resolve("test file with spaces.txt").toFile()
        testFile.writeText("content")

        val node = virtualFileSystem.getNode(testFile.absolutePath)

        assertEquals("test file with spaces.txt", node.name)
        assertEquals("content".length.toLong(), node.size)
    }
}