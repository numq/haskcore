package io.github.numq.haskcore.feature.editor.core

import arrow.core.right
import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.github.numq.haskcore.feature.editor.core.caret.Caret
import io.github.numq.haskcore.feature.editor.core.caret.CaretManager
import io.github.numq.haskcore.feature.editor.core.selection.Selection
import io.github.numq.haskcore.feature.editor.core.selection.SelectionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalEditorServiceTest {
    private val caretFlow = MutableStateFlow(Caret.ZERO)
    private val selectionFlow = MutableStateFlow(Selection.EMPTY)

    private val caretManager = mockk<CaretManager> {
        every { caret } returns caretFlow
        coEvery { handleTextEdit(any(), any()) } returns Unit.right()
        coEvery { moveTo(any(), any()) } returns Unit.right()
    }

    private val selectionManager = mockk<SelectionManager> {
        every { selection } returns selectionFlow
        coEvery { clearSelection() } returns Unit.right()
    }

    @Test
    fun `editor state should combine caret and selection updates`() = runTest {
        val service = LocalEditorService(backgroundScope, caretManager, selectionManager)

        val newCaret = Caret(TextPosition(1, 5))
        val newSelection = Selection.fromPositions(TextPosition(0, 0), TextPosition(1, 5))

        caretFlow.value = newCaret
        selectionFlow.value = newSelection

        runCurrent()

        assertEquals(newCaret, service.caret.value, "Caret should be updated in Editor state")
        assertEquals(newSelection, service.selection.value, "Selection should be updated in Editor state")
    }

    @Test
    fun `handleEdit should update caret and clear selection`() = runTest {
        val service = LocalEditorService(backgroundScope, caretManager, selectionManager)

        val editData = mockk<TextEdit.Data>()
        val snapshot = mockk<TextSnapshot>()
        val edit = mockk<TextEdit> {
            every { data } returns editData
        }

        service.handleEdit(snapshot, edit)

        coVerify(exactly = 1) {
            caretManager.handleTextEdit(snapshot, editData)
        }
        coVerify(exactly = 1) {
            selectionManager.clearSelection()
        }
    }

    @Test
    fun `requestHighlightingUpdate should update range`() = runTest {
        val service = LocalEditorService(backgroundScope, caretManager, selectionManager)

        service.requestHighlightingUpdate(10, 20)

        runCurrent()
        assertEquals(10..20, service.highlightingRange.value)
    }
}