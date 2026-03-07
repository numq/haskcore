package io.github.numq.haskcore.feature.editor.core.selection

import io.github.numq.haskcore.core.text.TextPosition
import io.github.numq.haskcore.core.text.TextSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSelectionManagerTest {
    @Test
    fun `selectWordAt should select Haskell identifier with prime`() = runTest {
        val manager = DefaultSelectionManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        val line = "map' f xs ="
        every { snapshot.getLineText(0) } returns line

        manager.selectWordAt(snapshot, TextPosition(0, 2))

        val selection = manager.selection.value
        assertEquals(TextPosition(0, 0), selection.range.start)
        assertEquals(TextPosition(0, 4), selection.range.end)
    }

    @Test
    fun `selectWordAt should select Haskell operator`() = runTest {
        val manager = DefaultSelectionManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        val line = "f >>> g"
        every { snapshot.getLineText(0) } returns line

        manager.selectWordAt(snapshot, TextPosition(0, 3))

        val selection = manager.selection.value
        assertEquals(TextPosition(0, 2), selection.range.start)
        assertEquals(TextPosition(0, 5), selection.range.end)
    }

    @Test
    fun `extendSelection should update selection from anchor`() = runTest {
        val manager = DefaultSelectionManager(backgroundScope)

        manager.startSelection(TextPosition(0, 5))

        manager.extendSelection(TextPosition(0, 0))

        val selection = manager.selection.value
        assertEquals(TextPosition(0, 0), selection.range.start)
        assertEquals(TextPosition(0, 5), selection.range.end)
        assertEquals(SelectionDirection.BACKWARD, selection.direction)
    }

    @Test
    fun `selectAll should cover entire content`() = runTest {
        val manager = DefaultSelectionManager(backgroundScope)
        val snapshot = mockk<TextSnapshot>()

        val lastPos = TextPosition(10, 20)
        every { snapshot.lastPosition } returns lastPos

        manager.selectAll(snapshot)

        val selection = manager.selection.value
        assertEquals(TextPosition.ZERO, selection.range.start)
        assertEquals(lastPos, selection.range.end)
    }

    @Test
    fun `clearSelection should reset state`() = runTest {
        val manager = DefaultSelectionManager(backgroundScope)

        manager.startSelection(TextPosition(0, 0))
        manager.clearSelection()

        assertEquals(Selection.EMPTY, manager.selection.value)
    }
}