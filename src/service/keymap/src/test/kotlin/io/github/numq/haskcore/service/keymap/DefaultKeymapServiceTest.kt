package io.github.numq.haskcore.service.keymap

import arrow.core.getOrElse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultKeymapServiceTest {
    private val testAction = "test.action"
    private val globalAction = "global.action"
    private val keyStroke = KeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK)

    private val testKeymapData = mapOf(
        KeymapContext.EDITOR to mapOf(keyStroke to testAction), KeymapContext.GLOBAL to mapOf(keyStroke to globalAction)
    )

    private val service = DefaultKeymapService(testKeymapData)

    @Test
    fun `getActionId should return local action if present`() {
        val result = service.getActionId(keyStroke, KeymapContext.EDITOR)

        assertTrue(result.isRight())
        assertEquals(testAction, result.getOrElse { null })
    }

    @Test
    fun `getActionId should fallback to global action if local is missing`() {
        val result = service.getActionId(keyStroke, KeymapContext.FILE_TREE)

        assertTrue(result.isRight())
        assertEquals(globalAction, result.getOrElse { null })
    }

    @Test
    fun `getActionId should return null if action not found anywhere`() {
        val unknownKey = KeyStroke(KeyEvent.VK_F12, 0)
        val result = service.getActionId(unknownKey, KeymapContext.EDITOR)

        assertTrue(result.isRight())
        assertEquals(null, result.getOrElse { "not-null" })
    }

    @Test
    fun `getKeyStrokes should return all distinct strokes for action`() = runTest {
        val duplicateStroke = KeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK)
        val dataWithDuplicates = mapOf(
            KeymapContext.EDITOR to mapOf(keyStroke to testAction),
            KeymapContext.GLOBAL to mapOf(duplicateStroke to testAction)
        )
        val customService = DefaultKeymapService(dataWithDuplicates)

        val result = customService.getKeyStrokes(testAction)

        assertTrue(result.isRight())
        result.onRight { strokes ->
            assertEquals(2, strokes.size)
            assertTrue(strokes.contains(keyStroke))
            assertTrue(strokes.contains(duplicateStroke))
        }
    }

    @Test
    fun `getKeyStrokes should return empty list for unknown action`() = runTest {
        val result = service.getKeyStrokes("unknown.id")

        assertTrue(result.isRight())
        result.onRight { strokes ->
            assertTrue(strokes.isEmpty())
        }
    }
}