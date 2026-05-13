package io.github.numq.haskcore.service.vfs

import app.cash.turbine.test
import arrow.core.right
import io.github.numq.haskcore.common.core.timestamp.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CachedVfsServiceTest {
    private val testScope = TestScope()
    private val vfsDataSource = mockk<VfsDataSource>()
    private lateinit var service: CachedVfsService

    @BeforeTest
    fun setup() {
        coEvery { vfsDataSource.getSnapshotData() } returns null.right()
        coEvery { vfsDataSource.updateSnapshotData(any()) } returns null.right()
        service = CachedVfsService(testScope, vfsDataSource)
    }

    @Test
    fun `observeFiles emits data after cache is populated`() = runTest {
        val path = "/test"
        val timestamp = Timestamp.now()
        val files = listOf(
            VirtualFile(
                path = "$path/f1.hs",
                name = "f1.hs",
                nameWithoutExtension = "f1",
                extension = "hs",
                isDirectory = false,
                isMetadata = false,
                size = 10,
                lastModifiedTimestamp = timestamp
            )
        )
        val eventFlow = MutableSharedFlow<VfsEvent>()

        coEvery { vfsDataSource.list(path) } returns files.right()
        coEvery { vfsDataSource.watch(path) } returns eventFlow.right()

        service.observeFiles(path).onRight { flow ->
            flow.test {
                runCurrent()

                val item = awaitItem()
                assertEquals(1, item.size)
                assertEquals("f1.hs", item.first().name)
            }
        }
    }

    @Test
    fun `observeVisibleFiles filters metadata correctly`() = runTest {
        val path = "/test"
        val timestamp = Timestamp.now()
        val files = listOf(
            VirtualFile(
                path = "$path/main.hs",
                name = "main.hs",
                nameWithoutExtension = "main",
                extension = "hs",
                isDirectory = false,
                isMetadata = false,
                size = 1,
                lastModifiedTimestamp = timestamp
            ), VirtualFile(
                path = "$path/.config",
                name = ".config",
                nameWithoutExtension = "",
                extension = null,
                isDirectory = true,
                isMetadata = true,
                size = 0,
                lastModifiedTimestamp = timestamp
            )
        )

        coEvery { vfsDataSource.list(path) } returns files.right()
        coEvery { vfsDataSource.watch(path) } returns flowOf<VfsEvent>().right()

        service.observeVisibleFiles(path).onRight { flow ->
            flow.test {
                runCurrent()

                val items = awaitItem()
                assertEquals(1, items.size)
                assertFalse(items.any { it.isMetadata })
            }
        }
    }

    @Test
    fun `delete updates cache actions`() = runTest {
        val path = "/test/file.hs"
        coEvery { vfsDataSource.delete(path) } returns Unit.right()

        val result = service.delete(path)

        assertTrue(result.isRight())
        coVerify { vfsDataSource.delete(path) }
    }

    @Test
    fun `backgroundSync triggers recursive listing`() = runTest {
        val root = "/project"
        val timestamp = Timestamp.now()
        val snapshotData = mockk<SnapshotData>()

        coEvery { snapshotData.toSnapshot() } returns Snapshot(root, timestamp, emptyMap())
        coEvery { vfsDataSource.getSnapshotData() } returns snapshotData.right()
        coEvery { vfsDataSource.listRecursive(root) } returns emptyList<VirtualFile>().right()

        CachedVfsService(testScope, vfsDataSource)

        testScope.runCurrent()

        coVerify(timeout = 2000) { vfsDataSource.listRecursive(root) }
    }
}