package io.github.numq.haskcore.service.vfs

import androidx.datastore.core.DataStore
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

internal class LocalVfsDataSourceTest {
    @TempDir
    lateinit var tempDir: Path

    private val testScope = TestScope()
    private val dataStore = mockk<DataStore<SnapshotData?>>(relaxed = true)
    private lateinit var dataSource: LocalVfsDataSource

    @BeforeTest
    fun setup() {
        dataSource = LocalVfsDataSource(testScope, dataStore)
    }

    @AfterTest
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `should create directory`() = runTest {
        val dirPath = tempDir.resolve("new_dir").absolutePathString()
        val result = dataSource.create(dirPath, isDirectory = true)

        assertTrue(result.isRight())
        assertTrue(Path.of(dirPath).isDirectory())
    }

    @Test
    fun `should create file with parent directories`() = runTest {
        val filePath = tempDir.resolve("nested/dir/file.txt").absolutePathString()
        val result = dataSource.create(filePath, isDirectory = false)

        assertTrue(result.isRight())
        assertTrue(Path.of(filePath).isRegularFile())
    }

    @Test
    fun `should list directory entries`() = runTest {
        tempDir.resolve("file1.hs").createFile()
        tempDir.resolve("file2.hs").createFile()

        val result = dataSource.list(tempDir.absolutePathString())

        result.onRight { files ->
            assertEquals(2, files.size)
            assertTrue(files.any { it.name == "file1.hs" })
        }
    }

    @Test
    fun `should delete file`() = runTest {
        val file = tempDir.resolve("to_delete.txt").createFile()

        val result = dataSource.delete(file.absolutePathString())

        assertTrue(result.isRight())
        assertFalse(file.exists())
    }

    @Test
    fun `should delete directory recursively`() = runTest {
        val dir = tempDir.resolve("dir_to_delete").createDirectory()
        dir.resolve("inner.txt").createFile()

        val result = dataSource.delete(dir.absolutePathString())

        assertTrue(result.isRight())
        assertFalse(dir.exists())
    }

    @Test
    fun `should move file`() = runTest {
        val src = tempDir.resolve("src.txt").createFile()
        val dst = tempDir.resolve("dst.txt")

        val result = dataSource.move(src.absolutePathString(), dst.absolutePathString(), overwrite = true)

        assertTrue(result.isRight())
        assertFalse(src.exists())
        assertTrue(dst.exists())
    }
}