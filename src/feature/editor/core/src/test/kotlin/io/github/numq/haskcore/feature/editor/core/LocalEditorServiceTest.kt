package io.github.numq.haskcore.feature.editor.core

import arrow.core.Either
import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextRevision
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.caret.CaretManager
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.core.selection.SelectionManager
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalEditorServiceTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var scope: CoroutineScope

    private lateinit var caretManager: CaretManager
    private lateinit var selectionManager: SelectionManager
    private lateinit var service: LocalEditorService

    private val caretFlow = MutableStateFlow(Caret(TextPosition(0, 0)))
    private val selectionFlow = MutableStateFlow(Selection.EMPTY)

    @BeforeEach
    fun setUp() {
        scope = CoroutineScope(testDispatcher)

        caretManager = mockk {
            every { caret } returns caretFlow
            coEvery { handleTextEdit(any(), any()) } returns Either.Right(Unit)
            coEvery { moveTo(any(), any()) } returns Either.Right(Unit)
            coEvery { moveLeft(any()) } returns Either.Right(Unit)
            coEvery { moveRight(any()) } returns Either.Right(Unit)
            coEvery { moveUp(any()) } returns Either.Right(Unit)
            coEvery { moveDown(any()) } returns Either.Right(Unit)
        }

        selectionManager = mockk {
            every { selection } returns selectionFlow
            coEvery { clearSelection() } returns Either.Right(Unit)
            coEvery { startSelection(any()) } returns Either.Right(Unit)
            coEvery { extendSelection(any()) } returns Either.Right(Unit)
            coEvery { selectAll(any()) } returns Either.Right(Unit)
        }

        service = LocalEditorService(scope, caretManager, selectionManager)
    }

    @Test
    fun `caret should be exposed from caret manager`() = runTest {
        assertEquals(caretFlow, service.caret)
    }

    @Test
    fun `selection should be exposed from selection manager`() = runTest {
        assertEquals(selectionFlow, service.selection)
    }

    @Test
    fun `editor state should update when caret and selection change`() = runTest {
        val newCaret = Caret(TextPosition(1, 5))
        val newSelection = Selection.fromPositions(TextPosition(0, 0), TextPosition(1, 5))

        caretFlow.value = newCaret
        selectionFlow.value = newSelection

        assertEquals(newCaret, service.caret.value, "Caret should be updated")
        assertEquals(newSelection, service.selection.value, "Selection should be updated")
    }

    @Test
    fun `handleEdit should clear selection and handle text edit`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val editData = TextEdit.Data.Single.Insert(
            startPosition = TextPosition.ZERO,
            newEndPosition = TextPosition(0, 4),
            insertedText = "test",
            startByte = 0,
            newEndByte = 4
        )
        val edit = TextEdit.User(revision = TextRevision.ZERO, data = editData)

        val result = service.handleEdit(snapshot, edit)

        assertTrue(result.isRight())
        coVerifyOrder {
            selectionManager.clearSelection()
            caretManager.handleTextEdit(snapshot, editData)
        }
    }

    @Test
    fun `handleEdit with Delete should work correctly`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val editData = TextEdit.Data.Single.Delete(
            startPosition = TextPosition(0, 0),
            oldEndPosition = TextPosition(0, 5),
            deletedText = "hello",
            startByte = 0,
            oldEndByte = 5
        )
        val edit = TextEdit.User(revision = TextRevision.ZERO, data = editData)

        val result = service.handleEdit(snapshot, edit)

        assertTrue(result.isRight())
        coVerifyOrder {
            selectionManager.clearSelection()
            caretManager.handleTextEdit(snapshot, editData)
        }
    }

    @Test
    fun `handleEdit with Replace should work correctly`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val editData = TextEdit.Data.Single.Replace(
            startPosition = TextPosition(0, 0),
            oldEndPosition = TextPosition(0, 5),
            newEndPosition = TextPosition(0, 3),
            oldText = "hello",
            newText = "hi",
            startByte = 0,
            oldEndByte = 5,
            newEndByte = 3
        )
        val edit = TextEdit.User(revision = TextRevision.ZERO, data = editData)

        val result = service.handleEdit(snapshot, edit)

        assertTrue(result.isRight())
        coVerifyOrder {
            selectionManager.clearSelection()
            caretManager.handleTextEdit(snapshot, editData)
        }
    }

    @Test
    fun `handleEdit with System edit should work correctly`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val editData = TextEdit.Data.Single.Insert(
            startPosition = TextPosition.ZERO,
            newEndPosition = TextPosition(0, 4),
            insertedText = "test",
            startByte = 0,
            newEndByte = 4
        )
        val edit = TextEdit.System(revision = TextRevision.ZERO, data = editData)

        val result = service.handleEdit(snapshot, edit)

        assertTrue(result.isRight())
        coVerifyOrder {
            selectionManager.clearSelection()
            caretManager.handleTextEdit(snapshot, editData)
        }
    }

    @Test
    fun `handleEdit with null edit should only clear selection`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.handleEdit(snapshot, null)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { selectionManager.clearSelection() }
        coVerify(exactly = 0) { caretManager.handleTextEdit(any(), any()) }
    }

    @Test
    fun `handleEdit with Batch should work correctly`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val editData = TextEdit.Data.Single.Insert(
            startPosition = TextPosition.ZERO,
            newEndPosition = TextPosition(0, 4),
            insertedText = "test",
            startByte = 0,
            newEndByte = 4
        )
        val edit = TextEdit.User(revision = TextRevision.ZERO, data = editData)

        val result = service.handleEdit(snapshot, edit)

        assertTrue(result.isRight())
        coVerifyOrder {
            selectionManager.clearSelection()
            caretManager.handleTextEdit(snapshot, editData)
        }
    }

    @Test
    fun `updateActiveLines should update range`() = runTest {
        val result = service.updateActiveLines(10, 20)

        assertTrue(result.isRight())
        assertEquals(10..20, service.activeLines.value)
    }

    @Test
    fun `updateActiveLines should be initially empty`() = runTest {
        assertEquals(IntRange.EMPTY, service.activeLines.value)
    }

    @Test
    fun `moveCaret should delegate to caretManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()
        val position = TextPosition(1, 2)

        val result = service.moveCaret(snapshot, position)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { caretManager.moveTo(snapshot, position) }
    }

    @Test
    fun `moveCaretLeft should delegate to caretManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.moveCaretLeft(snapshot)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { caretManager.moveLeft(snapshot) }
    }

    @Test
    fun `moveCaretRight should delegate to caretManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.moveCaretRight(snapshot)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { caretManager.moveRight(snapshot) }
    }

    @Test
    fun `moveCaretUp should delegate to caretManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.moveCaretUp(snapshot)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { caretManager.moveUp(snapshot) }
    }

    @Test
    fun `moveCaretDown should delegate to caretManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.moveCaretDown(snapshot)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { caretManager.moveDown(snapshot) }
    }

    @Test
    fun `startSelection should delegate to selectionManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()
        val position = TextPosition(1, 2)

        val result = service.startSelection(snapshot, position)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { selectionManager.startSelection(position) }
    }

    @Test
    fun `extendSelection should delegate to selectionManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()
        val position = TextPosition(1, 2)

        val result = service.extendSelection(snapshot, position)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { selectionManager.extendSelection(position) }
    }

    @Test
    fun `selectAll should delegate to selectionManager`() = runTest {
        val snapshot = mockk<TextSnapshot>()

        val result = service.selectAll(snapshot)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { selectionManager.selectAll(snapshot) }
    }

    @Test
    fun `clearSelection should delegate to selectionManager`() = runTest {
        val result = service.clearSelection()

        assertTrue(result.isRight())
        coVerify(exactly = 1) { selectionManager.clearSelection() }
    }

    @Test
    fun `close should cancel scope`() = runTest {
        assertTrue(scope.coroutineContext.isActive)

        service.close()

        assertTrue(!scope.coroutineContext.isActive)
    }
}