package io.github.numq.haskcore.explorer

import io.github.numq.haskcore.clipboard.ClipboardService
import io.github.numq.haskcore.filesystem.FileSystemService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerRepositoryTest {
    @TempDir
    private lateinit var tempDir: File

    private lateinit var fileSystemService: FileSystemService
    private lateinit var clipboardService: ClipboardService
    private lateinit var explorerRepository: ExplorerRepository
    private val rootPath = "/root"

    @BeforeEach
    fun setup() {
        fileSystemService = mockk()
        clipboardService = mockk()
        explorerRepository = ExplorerRepository.Default(rootPath, clipboardService, fileSystemService)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getNodes throws when listDirectory fails`() = runTest {
        coEvery { fileSystemService.listDirectory(rootPath) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.getNodes(rootPath)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `getNodes handles errors in buildNode`() = runTest {
        val filePath = File(tempDir, "$rootPath/file.txt").path

        coEvery { fileSystemService.listDirectory(rootPath) } returns Result.success(listOf(filePath))
        coEvery { fileSystemService.isFile(filePath) } returns Result.failure(RuntimeException("file fail"))
        coEvery { fileSystemService.isDirectory(filePath) } returns Result.success(false)
        coEvery { fileSystemService.observeDirectoryChanges(rootPath) } returns Result.success(flowOf())

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

        val result = explorerRepository.createFile(rootPath, "file")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `createDirectory throws when filesystem fails`() = runTest {
        coEvery { fileSystemService.createDirectory(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.createDirectory(rootPath, "dir")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `renameNode throws when filesystem fails`() = runTest {
        val node = ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, false, "txt")
        coEvery { fileSystemService.rename(node.path, "new") } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.renameNode(node, "new")
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `cutNodes throws when clipboard fails`() = runTest {
        val nodes = listOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, false, "txt"))
        coEvery { clipboardService.cut(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.cutNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `copyNodes throws when clipboard fails`() = runTest {
        val nodes = listOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, false, "txt"))
        coEvery { clipboardService.copy(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.copyNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `pasteNodes throws when clipboard fails`() = runTest {
        val dir = ExplorerNode.Directory("dir", "$rootPath/dir", rootPath, 1, 0L, false, false)
        coEvery { clipboardService.paste(any()) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.pasteNodes(dir)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `moveNodes throws when filesystem fails`() = runTest {
        val dest = ExplorerNode.Directory("dir", "$rootPath/dir", rootPath, 1, 0L, false, false)
        val nodes = listOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, false, "txt"))

        coEvery { fileSystemService.move(nodes[0].path, "$rootPath/dir/file", false) } returns Result.failure(
            RuntimeException("fail")
        )

        val result = explorerRepository.moveNodes(nodes, dest)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }

    @Test
    fun `deleteNodes throws when filesystem fails`() = runTest {
        val nodes = listOf(ExplorerNode.File("file", "$rootPath/file", rootPath, 1, 0L, false, "txt"))
        coEvery { fileSystemService.delete(nodes[0].path) } returns Result.failure(RuntimeException("fail"))

        val result = explorerRepository.deleteNodes(nodes)
        assertFailsWith<RuntimeException> { result.getOrThrow() }
    }
}
