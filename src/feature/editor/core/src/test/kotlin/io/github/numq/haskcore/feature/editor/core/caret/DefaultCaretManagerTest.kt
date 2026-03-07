package io.github.numq.haskcore.feature.editor.core.caret

import io.github.numq.haskcore.core.text.TextEdit
import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultCaretManagerTest {
    @Test
    fun `moveLeft should update position correctly`() = runTest {
        val manager = DefaultCaretManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveLeft(snapshot)

        assertEquals(TextPosition(0, 4), manager.caret.value.position)
    }

    @Test
    fun `moveRight should stay at end of line`() = runTest {
        val manager = DefaultCaretManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 5

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveRight(snapshot)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should shift caret after insertion`() = runTest {
        val manager = DefaultCaretManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 2))

        val insertEdit = TextEdit.Data.Single.Insert(
            startPosition = TextPosition(0, 0),
            newEndPosition = TextPosition(0, 3),
            insertedText = "abc",
            startByte = 0,
            newEndByte = 3
        )

        manager.handleTextEdit(snapshot, insertEdit)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should move caret to new position if edit covers current position`() = runTest {
        val manager = DefaultCaretManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 20

        manager.moveTo(snapshot, TextPosition(0, 5))

        val deleteEdit = TextEdit.Data.Single.Delete(
            startPosition = TextPosition(0, 0),
            oldEndPosition = TextPosition(0, 10),
            deletedText = "0123456789",
            startByte = 0,
            oldEndByte = 10
        )

        manager.handleTextEdit(snapshot, deleteEdit)

        assertEquals(TextPosition(0, 0), manager.caret.value.position)
    }

    @Test
    fun `sticky column should be preserved when moving up and down`() = runTest {
        val manager = DefaultCaretManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        every { snapshot.lines } returns 3
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 2
        every { snapshot.getLineLength(2) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 8))

        manager.moveDown(snapshot)
        assertEquals(TextPosition(1, 2), manager.caret.value.position)

        manager.moveDown(snapshot)
        assertEquals(TextPosition(2, 8), manager.caret.value.position)
    }
}