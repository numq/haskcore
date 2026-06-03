package io.github.numq.haskcore.feature.editor.core.caret

import io.github.numq.haskcore.common.core.text.TextEdit
import io.github.numq.haskcore.common.core.text.TextPosition
import io.github.numq.haskcore.common.core.text.TextSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultCaretManagerTest {
    private val scope = TestScope(StandardTestDispatcher())

    private lateinit var manager: DefaultCaretManager
    private lateinit var snapshot: TextSnapshot

    @BeforeEach
    fun setUp() {
        manager = DefaultCaretManager(scope)
        snapshot = mockk()
    }

    @Test
    fun `moveTo should set caret to valid position`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveTo should clamp position to valid bounds`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 5

        manager.moveTo(snapshot, TextPosition(0, 10))

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveTo should clamp line to valid bounds`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 5
        every { snapshot.getLineLength(1) } returns 3

        manager.moveTo(snapshot, TextPosition(5, 0))

        assertEquals(TextPosition(1, 0), manager.caret.value.position)
    }

    @Test
    fun `moveLeft should move one column left`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveLeft(snapshot)

        assertEquals(TextPosition(0, 4), manager.caret.value.position)
    }

    @Test
    fun `moveLeft should move to previous line end when at column 0`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 5
        every { snapshot.getLineLength(1) } returns 3

        manager.moveTo(snapshot, TextPosition(1, 0))
        manager.moveLeft(snapshot)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveLeft should not move at start of document`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 0))
        manager.moveLeft(snapshot)

        assertEquals(TextPosition(0, 0), manager.caret.value.position)
    }

    @Test
    fun `moveRight should move one column right`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveRight(snapshot)

        assertEquals(TextPosition(0, 6), manager.caret.value.position)
    }

    @Test
    fun `moveRight should move to next line start at line end`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 5
        every { snapshot.getLineLength(1) } returns 3

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveRight(snapshot)

        assertEquals(TextPosition(1, 0), manager.caret.value.position)
    }

    @Test
    fun `moveRight should stay at end of document`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 5

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveRight(snapshot)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveUp should move to same column on previous line`() = runTest {
        every { snapshot.lines } returns 3
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 10
        every { snapshot.getLineLength(2) } returns 10

        manager.moveTo(snapshot, TextPosition(1, 5))
        manager.moveUp(snapshot)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveUp should clamp column to previous line length`() = runTest {
        every { snapshot.lines } returns 3
        every { snapshot.getLineLength(0) } returns 3
        every { snapshot.getLineLength(1) } returns 10
        every { snapshot.getLineLength(2) } returns 10

        manager.moveTo(snapshot, TextPosition(1, 8))
        manager.moveUp(snapshot)

        assertEquals(TextPosition(0, 3), manager.caret.value.position)
    }

    @Test
    fun `moveUp should not move at first line`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveUp(snapshot)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `moveDown should move to same column on next line`() = runTest {
        every { snapshot.lines } returns 3
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 10
        every { snapshot.getLineLength(2) } returns 10

        manager.moveTo(snapshot, TextPosition(1, 5))
        manager.moveDown(snapshot)

        assertEquals(TextPosition(2, 5), manager.caret.value.position)
    }

    @Test
    fun `moveDown should clamp column to next line length`() = runTest {
        every { snapshot.lines } returns 3
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 3
        every { snapshot.getLineLength(2) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 8))
        manager.moveDown(snapshot)

        assertEquals(TextPosition(1, 3), manager.caret.value.position)
    }

    @Test
    fun `moveDown should not move at last line`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(1) } returns 10

        manager.moveTo(snapshot, TextPosition(1, 5))
        manager.moveDown(snapshot)

        assertEquals(TextPosition(1, 5), manager.caret.value.position)
    }

    @Test
    fun `sticky column should be preserved when moving up and down`() = runTest {
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

    @Test
    fun `sticky column should be updated on moveTo`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 3))
        manager.moveDown(snapshot)

        assertEquals(TextPosition(1, 3), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should shift caret after insertion`() = runTest {
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
    fun `handleTextEdit should not move caret when edit is after caret`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 20

        manager.moveTo(snapshot, TextPosition(0, 2))

        val insertEdit = TextEdit.Data.Single.Insert(
            startPosition = TextPosition(0, 5),
            newEndPosition = TextPosition(0, 8),
            insertedText = "abc",
            startByte = 5,
            newEndByte = 8
        )

        manager.handleTextEdit(snapshot, insertEdit)

        assertEquals(TextPosition(0, 2), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should adjust caret when edit is before caret`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 20

        manager.moveTo(snapshot, TextPosition(0, 10))

        val insertEdit = TextEdit.Data.Single.Insert(
            startPosition = TextPosition(0, 5),
            newEndPosition = TextPosition(0, 8),
            insertedText = "abc",
            startByte = 5,
            newEndByte = 8
        )

        manager.handleTextEdit(snapshot, insertEdit)

        assertEquals(TextPosition(0, 13), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should handle insertion with newline`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 5
        every { snapshot.getLineLength(1) } returns 0

        manager.moveTo(snapshot, TextPosition(0, 5))

        val insertNewline = TextEdit.Data.Single.Insert(
            startPosition = TextPosition(0, 5),
            newEndPosition = TextPosition(1, 0),
            insertedText = "\n",
            startByte = 5,
            newEndByte = 6
        )

        manager.handleTextEdit(snapshot, insertNewline)

        assertEquals(TextPosition(1, 0), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should handle replace with newline`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 3
        every { snapshot.getLineLength(1) } returns 0

        manager.moveTo(snapshot, TextPosition(0, 5))

        val replaceWithNewline = TextEdit.Data.Single.Replace(
            startPosition = TextPosition(0, 3),
            oldEndPosition = TextPosition(0, 7),
            newEndPosition = TextPosition(1, 0),
            oldText = "test",
            newText = "\n",
            startByte = 3,
            oldEndByte = 7,
            newEndByte = 4
        )

        manager.handleTextEdit(snapshot, replaceWithNewline)

        assertEquals(TextPosition(1, 0), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should handle batch operations`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 20

        manager.moveTo(snapshot, TextPosition(0, 5))

        val batchEdit = TextEdit.Data.Batch(
            singles = listOf(
                TextEdit.Data.Single.Insert(
                    startPosition = TextPosition(0, 0),
                    newEndPosition = TextPosition(0, 3),
                    insertedText = "abc",
                    startByte = 0,
                    newEndByte = 3
                ), TextEdit.Data.Single.Insert(
                    startPosition = TextPosition(0, 5),
                    newEndPosition = TextPosition(0, 8),
                    insertedText = "xyz",
                    startByte = 5,
                    newEndByte = 8
                )
            )
        )

        manager.handleTextEdit(snapshot, batchEdit)

        assertEquals(TextPosition(0, 11), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should handle delete that spans multiple lines`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 5

        manager.moveTo(snapshot, TextPosition(2, 2))

        val deleteEdit = TextEdit.Data.Single.Delete(
            startPosition = TextPosition(0, 3),
            oldEndPosition = TextPosition(2, 1),
            deletedText = "lo\nWorl",
            startByte = 3,
            oldEndByte = 10
        )

        manager.handleTextEdit(snapshot, deleteEdit)

        assertEquals(TextPosition(0, 2), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should move caret to start when inside deleted range`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 5

        manager.moveTo(snapshot, TextPosition(1, 2))

        val deleteEdit = TextEdit.Data.Single.Delete(
            startPosition = TextPosition(0, 3),
            oldEndPosition = TextPosition(2, 1),
            deletedText = "lo\nWorl",
            startByte = 3,
            oldEndByte = 10
        )

        manager.handleTextEdit(snapshot, deleteEdit)

        assertEquals(TextPosition(0, 2), manager.caret.value.position)
    }

    @Test
    fun `handleTextEdit should handle replace at caret position`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))

        val replaceEdit = TextEdit.Data.Single.Replace(
            startPosition = TextPosition(0, 5),
            oldEndPosition = TextPosition(0, 5),
            newEndPosition = TextPosition(0, 8),
            oldText = "",
            newText = "abc",
            startByte = 5,
            oldEndByte = 5,
            newEndByte = 8
        )

        manager.handleTextEdit(snapshot, replaceEdit)

        assertEquals(TextPosition(0, 8), manager.caret.value.position)
    }

    @Test
    fun `sticky column should update after handleTextEdit`() = runTest {
        every { snapshot.lines } returns 2
        every { snapshot.getLineLength(0) } returns 10
        every { snapshot.getLineLength(1) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 8))

        val deleteEdit = TextEdit.Data.Single.Delete(
            startPosition = TextPosition(0, 5),
            oldEndPosition = TextPosition(0, 9),
            deletedText = "test",
            startByte = 5,
            oldEndByte = 9
        )

        manager.handleTextEdit(snapshot, deleteEdit)

        assertEquals(TextPosition(0, 5), manager.caret.value.position)

        manager.moveDown(snapshot)
        assertEquals(TextPosition(1, 5), manager.caret.value.position)
    }

    @Test
    fun `should handle empty snapshot`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 0

        manager.moveTo(snapshot, TextPosition(0, 0))

        assertEquals(TextPosition(0, 0), manager.caret.value.position)

        manager.moveRight(snapshot)
        assertEquals(TextPosition(0, 0), manager.caret.value.position)
    }

    @Test
    fun `should handle snapshot with single line`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 10

        manager.moveTo(snapshot, TextPosition(0, 5))
        manager.moveUp(snapshot)
        assertEquals(TextPosition(0, 5), manager.caret.value.position)

        manager.moveDown(snapshot)
        assertEquals(TextPosition(0, 5), manager.caret.value.position)
    }

    @Test
    fun `should handle large line lengths correctly`() = runTest {
        every { snapshot.lines } returns 1
        every { snapshot.getLineLength(0) } returns 1_000_000

        manager.moveTo(snapshot, TextPosition(0, 999_999))

        manager.moveRight(snapshot)
        assertEquals(TextPosition(0, 1_000_000), manager.caret.value.position)

        manager.moveLeft(snapshot)
        assertEquals(TextPosition(0, 999_999), manager.caret.value.position)
    }
}