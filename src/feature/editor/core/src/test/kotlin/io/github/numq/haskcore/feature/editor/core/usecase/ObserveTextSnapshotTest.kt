package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.right
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextOperation
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.core.timestamp.Timestamp
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.service.document.Document
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.text.TextService
import io.github.numq.haskcore.service.vfs.VfsService
import io.github.numq.haskcore.service.vfs.VirtualFile
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class ObserveTextSnapshotTest {
    private val path = "test.hs"
    private val editorService = mockk<EditorService>(relaxed = true)
    private val documentService = mockk<DocumentService>()
    private val journalService = mockk<JournalService>(relaxed = true)
    private val textService = mockk<TextService>()
    private val vfsService = mockk<VfsService>()

    private val editsFlow = MutableSharedFlow<TextEdit?>(extraBufferCapacity = 1)
    private val snapshotFlow = MutableStateFlow<TextSnapshot?>(null)

    private fun createSnapshotMock(content: String, rev: Long = 0L): TextSnapshot = mockk {
        every { text } returns content
        every { revision } returns rev
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()

        mockkStatic(Path::class)

        mockkStatic("kotlin.io.path.PathsKt")

        val mockPath = mockk<Path>(relaxed = true)
        val mockParent = mockk<Path>(relaxed = true)

        every { Path.of(path) } returns mockPath
        every { mockPath.parent } returns mockParent
        every { mockParent.toString() } returns "test_dir"

        val mockTime = FileTime.from(1000, TimeUnit.NANOSECONDS)
        every { mockPath.getLastModifiedTime() } returns mockTime

        every { textService.edits } returns editsFlow
        every { textService.snapshot } returns snapshotFlow

        coEvery { documentService.readDocument(path) } returns Document(
            path, "test.hs", "initial content", false
        ).right()
        coEvery { textService.initialize(any()) } returns Unit.right()
    }

    @Test
    fun `should auto-save document after sampling period`() = runTest {
        val useCase = ObserveTextSnapshot(path, editorService, documentService, journalService, textService, vfsService)
        coEvery { vfsService.observeDirectory(any()) } returns flowOf(emptyList<VirtualFile>()).right()
        coEvery { documentService.saveDocument(any(), any()) } returns Unit.right()

        snapshotFlow.value = createSnapshotMock("initial")

        val flow = useCase(Unit).getOrElse { throw it }
        val job = backgroundScope.launch { flow.collect() }
        runCurrent()

        val newText = "updated content"
        snapshotFlow.value = createSnapshotMock(newText, 1L)

        advanceTimeBy(2001)
        runCurrent()

        coVerify(exactly = 1) { documentService.saveDocument(path, newText) }
        job.cancel()
    }

    @Test
    fun `should reload from disk when external change detected`() = runTest {
        val vfsFlow = MutableSharedFlow<List<VirtualFile>>()

        coEvery { vfsService.observeDirectory(any()) } returns vfsFlow.right()
        coEvery { documentService.saveDocument(any(), any()) } returns Unit.right()
        coEvery { journalService.clear() } returns Unit.right()
        coEvery { journalService.push(any()) } returns Unit.right()
        coEvery { editorService.handleEdit(any(), any()) } returns Unit.right()

        val currentContent = "line 1\n"
        val externalContent = "line 1\nline 2\n"

        snapshotFlow.value = createSnapshotMock(currentContent)

        coEvery { documentService.readDocument(path) } returns Document(path, "test.hs", externalContent, false).right()
        coEvery { textService.execute(any()) } returns Unit.right()

        val useCase = ObserveTextSnapshot(path, editorService, documentService, journalService, textService, vfsService)
        val flow = useCase(Unit).getOrElse { throw it }
        val job = backgroundScope.launch { flow.collect() }

        runCurrent()

        val modifiedFile = VirtualFile(
            path = path,
            name = "test.hs",
            extension = "hs",
            isDirectory = false,
            size = 100L,
            lastModified = Timestamp(nanoseconds = 5000L)
        )

        vfsFlow.emit(listOf(modifiedFile))

        runCurrent()

        val operationSlot = slot<TextOperation.System>()
        coVerify(exactly = 1) { textService.execute(capture(operationSlot)) }

        val batch = operationSlot.captured.data as TextOperation.Data.Batch
        val firstOp = batch.operations.first()

        assert(firstOp is TextOperation.Data.Single.Insert)
        assertEquals("line 2\n", (firstOp as TextOperation.Data.Single.Insert).text)

        job.cancel()
    }
}