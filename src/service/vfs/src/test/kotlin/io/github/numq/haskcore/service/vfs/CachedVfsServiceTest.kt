package io.github.numq.haskcore.service.vfs

import arrow.core.Either
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CachedVfsServiceTest {
    private val vfsDataSource = mockk<VfsDataSource>()
    private val testPath = "/test/dir"

    @Test
    fun `observeDirectory should populate cache on first call`() = runTest(UnconfinedTestDispatcher()) {
        val service = CachedVfsService(backgroundScope, vfsDataSource)
        val initialFiles = listOf(
            VirtualFile(
                path = "$testPath/file.hs",
                name = "file.hs",
                extension = "hs",
                isDirectory = false,
                size = 100,
                lastModified = Timestamp(0)
            )
        )

        coEvery { vfsDataSource.list(testPath) } returns Either.Right(initialFiles)
        coEvery { vfsDataSource.watch(testPath) } returns Either.Right(MutableSharedFlow())

        val flow = service.observeDirectory(testPath).getOrNull()!!

        runCurrent()

        val result = flow.first()

        Assertions.assertEquals(initialFiles, result)
        coVerify(exactly = 1) { vfsDataSource.list(testPath) }
    }

    @Test
    fun `cache should update on Deleted event`() = runTest(UnconfinedTestDispatcher()) {
        val service = CachedVfsService(backgroundScope, vfsDataSource)
        val file = VirtualFile(
            path = "$testPath/to_delete.hs",
            name = "to_delete.hs",
            extension = "hs",
            isDirectory = false,
            size = 10,
            lastModified = Timestamp(0)
        )
        val events = MutableSharedFlow<VfsEvent>(replay = 1)

        coEvery { vfsDataSource.list(testPath) } returns Either.Right(listOf(file))
        coEvery { vfsDataSource.watch(testPath) } returns Either.Right(events)

        val flow = service.observeDirectory(testPath).getOrNull()!!
        runCurrent()

        events.emit(VfsEvent.Deleted("$testPath/to_delete.hs"))
        runCurrent()

        val result = flow.first()
        Assertions.assertEquals(0, result.size)
    }
}