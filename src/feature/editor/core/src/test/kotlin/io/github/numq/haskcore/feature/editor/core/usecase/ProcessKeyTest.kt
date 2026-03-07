package io.github.numq.haskcore.feature.editor.core.usecase

import arrow.core.getOrElse
import arrow.core.right
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextOperation
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.EditorService
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.service.document.DocumentService
import io.github.numq.haskcore.service.journal.JournalService
import io.github.numq.haskcore.service.keymap.KeymapService
import io.github.numq.haskcore.service.text.TextService
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

internal class ProcessKeyTest {
    private val path = "test.hs"
    private val editorService = mockk<EditorService>(relaxed = true)
    private val documentService = mockk<DocumentService>(relaxed = true)
    private val journalService = mockk<JournalService>(relaxed = true)
    private val keymapService = mockk<KeymapService>()
    private val textService = mockk<TextService>(relaxed = true)

    private val snapshotFlow = MutableStateFlow<TextSnapshot?>(null)
    private val caretStateFlow = MutableStateFlow(Caret.ZERO)
    private val selectionStateFlow = MutableStateFlow(Selection.EMPTY)

    init {
        every { textService.snapshot } returns snapshotFlow
        every { editorService.caret } returns caretStateFlow
        every { editorService.selection } returns selectionStateFlow
    }

    @Test
    fun `should handle printable character input`() = runTest {
        val useCase = ProcessKey(path, editorService, documentService, journalService, keymapService, textService)

        val snapshot = mockk<TextSnapshot> {
            every { isValidPosition(any()) } returns true
        }
        snapshotFlow.value = snapshot

        val input = ProcessKey.Input(KeyEvent.VK_A, 0, 'A'.toInt())

        every { keymapService.getActionId(any(), any()) } returns null.right()
        coEvery { textService.execute(any()) } returns Unit.right()

        caretStateFlow.value = Caret(TextPosition(0, 0))

        useCase(input).getOrElse { throw it }

        val opSlot = slot<TextOperation.User>()
        coVerify { textService.execute(capture(opSlot)) }

        val data = opSlot.captured.data as TextOperation.Data.Single.Insert
        assertEquals("A", data.text)
        assertEquals(0, data.position.column)
    }

    @Test
    fun `should handle backspace across lines`() = runTest {
        val useCase = ProcessKey(path, editorService, documentService, journalService, keymapService, textService)

        caretStateFlow.value = Caret(TextPosition(1, 0))

        val snapshot = mockk<TextSnapshot> {
            every { getLineLength(0) } returns 5
            every { getLineLength(1) } returns 0
            every { text } returns "12345\n"
            every { isValidPosition(any()) } returns true
        }
        snapshotFlow.value = snapshot

        every { keymapService.getActionId(any(), any()) } returns "editor.action.backspace".right()
        coEvery { textService.execute(any()) } returns Unit.right()

        useCase(ProcessKey.Input(KeyEvent.VK_BACK_SPACE, 0, 0)).getOrElse { throw it }

        val opSlot = slot<TextOperation.User>()
        coVerify { textService.execute(capture(opSlot)) }

        val data = opSlot.captured.data as TextOperation.Data.Single.Delete
        assertEquals(0, data.range.start.line)
        assertEquals(5, data.range.start.column)
        assertEquals(1, data.range.end.line)
        assertEquals(0, data.range.end.column)
    }

    @Test
    fun `should handle undo action`() = runTest {
        val useCase = ProcessKey(path, editorService, documentService, journalService, keymapService, textService)

        val currentRevision = 10L
        val snapshot = mockk<TextSnapshot>(relaxed = true) {
            every { revision } returns currentRevision
            every { isValidPosition(any()) } returns true
            every { lines } returns 1
        }
        snapshotFlow.value = snapshot

        every { keymapService.getActionId(any(), any()) } returns "editor.action.undo".right()

        val editData = TextEdit.Data.Single.Insert(
            startPosition = TextPosition.ZERO,
            newEndPosition = TextPosition(0, 4),
            insertedText = "text",
            startByte = 0,
            newEndByte = 4
        )

        val editToUndo = TextEdit.User(data = editData, revision = currentRevision)

        coEvery { journalService.undo(currentRevision) } returns (editToUndo as TextEdit).right()

        coEvery { textService.execute(any()) } returns Unit.right()

        useCase(
            ProcessKey.Input(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, 0)
        ).getOrElse { throw it }

        coVerify { journalService.undo(currentRevision) }

        coVerify { textService.execute(match { it is TextOperation.System }) }
    }
}