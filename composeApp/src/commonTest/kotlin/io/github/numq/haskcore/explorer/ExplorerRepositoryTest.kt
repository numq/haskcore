package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.clipboard.ClipboardService
import io.github.numq.haskcore.filesystem.FileSystemService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerRepositoryTest {
    private lateinit var fileSystemService: FileSystemService
    private lateinit var clipboardService: ClipboardService
    private lateinit var explorerRepository: ExplorerRepository

    @TempDir
    private lateinit var tempDir: File

    private lateinit var rootPath: String

    private val destination = ExplorerNode.Directory(
        name = "directory",
        path = "/destination",
        parentPath = "/root",
        depth = 0,
        lastModified = Long.MAX_VALUE,
        expanded = false
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        fileSystemService = mockk()
        clipboardService = mockk()
        explorerRepository = ExplorerRepository.Default(clipboardService, fileSystemService)
        rootPath = File(tempDir, "/root").also(File::mkdir).path
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getNodes throws when listDirectory fails`() = runTest {
        coEvery { fileSystemService.listDirectory(rootPath) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.getNodes(rootPath)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `getNodes handles errors in observeDirectoryChanges`() = runTest {
        coEvery { fileSystemService.listDirectory(rootPath) } returns Result.success(emptyList())
        coEvery { fileSystemService.isFile(rootPath) } returns Result.success(false)
        coEvery { fileSystemService.isDirectory(rootPath) } returns Result.success(true)
        coEvery { fileSystemService.observeDirectoryChanges(rootPath) } returns Result.failure(RuntimeException("fail"))

        val flow = explorerRepository.getNodes(rootPath)
        assertFailsWith<RuntimeException> { flow.getOrThrow().first() }
    }

    @Test
    fun `createFile throws when filesystem fails`() = runTest {
        coEvery {
            fileSystemService.createFile(
                any(),
                any<ByteArray>()
            )
        } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.createFile(destination, "file")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `createDirectory throws when filesystem fails`() = runTest {
        coEvery { fileSystemService.createDirectory(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.createDirectory(destination, "dir")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `renameNode throws when filesystem fails`() = runTest {
        val node = ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, "txt")
        coEvery { fileSystemService.rename(node.path, "new") } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.renameNode(node, "new")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `cutNodes throws when clipboard fails`() = runTest {
        val nodes = setOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, "txt"))
        coEvery { clipboardService.cut(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.cutNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `copyNodes throws when clipboard fails`() = runTest {
        val nodes = setOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, "txt"))
        coEvery { clipboardService.copy(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.copyNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `pasteNodes throws when clipboard fails`() = runTest {
        val dir = ExplorerNode.Directory("dir", "$rootPath/dir", rootPath, 1, 0L, false)
        coEvery { clipboardService.paste(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.pasteNodes(dir)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `moveNodes throws when filesystem fails`() = runTest {
        val dest = ExplorerNode.Directory("dir", "$rootPath/dir", rootPath, 1, 0L, false)
        val nodes = setOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, "txt"))

        coEvery { fileSystemService.move(nodes.first().path, "$rootPath/dir/file", false) } returns Result.failure(
            RuntimeException("fail")
        )

        val result = explorerRepository.moveNodes(nodes, dest)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `deleteNodes throws when filesystem fails`() = runTest {
        val nodes = setOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, "txt"))
        coEvery { fileSystemService.delete(nodes.first().path) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.deleteNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }
}
