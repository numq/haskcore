package io.github.numq.haskcore.service.keymap

import arrow.core.getOrElse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

internal class DefaultKeymapServiceTest {
    private val editorActions = setOf(
        KeymapAction.File.Save,
        KeymapAction.File.SelectAll,
        KeymapAction.Clipboard.Copy,
        KeymapAction.Navigation.Move.Left,
        KeymapAction.Editing.Basic.Enter,
    )

    private val globalActions = setOf(KeymapAction.File.Save, KeymapAction.History.Undo)

    private val testActionsByContext = mapOf(
        KeymapContext.EDITOR to editorActions, KeymapContext.GLOBAL to globalActions
    )

    private val service = DefaultKeymapService(testActionsByContext)

    @Test
    fun `findAction should return local action if present in context`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_S, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertEquals(KeymapAction.File.Save, result.getOrElse { null })
    }

    @Test
    fun `findAction should return action with normalized modifiers`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_S,
            modifiers = KeyEvent.CTRL_DOWN_MASK or KeyEvent.KEY_LOCATION_LEFT,
            context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertEquals(KeymapAction.File.Save, result.getOrElse { null })
    }

    @Test
    fun `findAction should fallback to global action if local is missing`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_Z, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertEquals(KeymapAction.History.Undo, result.getOrElse { null })
    }

    @Test
    fun `findAction should return global action when context is FILE_TREE`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_S, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.FILE_TREE
        )

        assertTrue(result.isRight())
        assertEquals(KeymapAction.File.Save, result.getOrElse { null })
    }

    @Test
    fun `findAction should return null if action not found anywhere`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_F12, modifiers = 0, context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertNull(result.getOrElse { KeymapAction.File.Save })
    }

    @Test
    fun `findAction should return null for GLOBAL context if not found`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_C, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.GLOBAL
        )

        assertTrue(result.isRight())
        assertNull(result.getOrElse { KeymapAction.File.Save })
    }

    @Test
    fun `findAction should not fallback to GLOBAL if already searching in GLOBAL`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_F1, modifiers = 0, context = KeymapContext.GLOBAL
        )

        assertTrue(result.isRight())
        assertNull(result.getOrElse { KeymapAction.File.Save })
    }

    @Test
    fun `findAction should correctly identify arrow keys`() = runTest {
        val result = service.findAction(
            keyCode = KeyEvent.VK_LEFT, modifiers = 0, context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertEquals(KeymapAction.Navigation.Move.Left, result.getOrElse { null })
    }

    @Test
    fun `findAction should distinguish between different modifiers for same key`() = runTest {
        val resultWithCtrl = service.findAction(
            keyCode = KeyEvent.VK_A, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.EDITOR
        )

        assertTrue(resultWithCtrl.isRight())
        assertEquals(KeymapAction.File.SelectAll, resultWithCtrl.getOrElse { null })

        val resultWithoutCtrl = service.findAction(
            keyCode = KeyEvent.VK_A, modifiers = 0, context = KeymapContext.EDITOR
        )

        assertTrue(resultWithoutCtrl.isRight())
        assertNull(resultWithoutCtrl.getOrElse { KeymapAction.File.Save })
    }

    @Test
    fun `findAction should handle Shift modifier correctly`() = runTest {
        val actionsWithShift = mapOf(
            KeymapContext.EDITOR to setOf(
                KeymapAction.Navigation.Move.LeftWithSelection,
                KeymapAction.Navigation.Move.Left,
            )
        )
        val shiftService = DefaultKeymapService(actionsWithShift)

        val shiftResult = shiftService.findAction(
            keyCode = KeyEvent.VK_LEFT, modifiers = KeyEvent.SHIFT_DOWN_MASK, context = KeymapContext.EDITOR
        )
        assertTrue(shiftResult.isRight())
        assertEquals(KeymapAction.Navigation.Move.LeftWithSelection, shiftResult.getOrElse { null })

        val plainResult = shiftService.findAction(
            keyCode = KeyEvent.VK_LEFT, modifiers = 0, context = KeymapContext.EDITOR
        )
        assertTrue(plainResult.isRight())
        assertEquals(KeymapAction.Navigation.Move.Left, plainResult.getOrElse { null })
    }

    @Test
    fun `findAction should handle empty actions set`() = runTest {
        val emptyService = DefaultKeymapService(
            mapOf(KeymapContext.EDITOR to emptySet())
        )

        val result = emptyService.findAction(
            keyCode = KeyEvent.VK_ENTER, modifiers = 0, context = KeymapContext.EDITOR
        )

        assertTrue(result.isRight())
        assertNull(result.getOrElse { KeymapAction.File.Save })
    }

    @Test
    fun `findAction should handle multiple contexts correctly`() = runTest {
        val multiContextService = DefaultKeymapService(
            mapOf(
                KeymapContext.EDITOR to setOf(
                    KeymapAction.Editing.Basic.Enter,
                    KeymapAction.Clipboard.Copy,
                ), KeymapContext.FILE_TREE to setOf(
                    KeymapAction.Navigation.Move.Up,
                ), KeymapContext.GLOBAL to setOf(
                    KeymapAction.File.Save,
                )
            )
        )

        val editorResult = multiContextService.findAction(
            keyCode = KeyEvent.VK_ENTER, modifiers = 0, context = KeymapContext.EDITOR
        )
        assertEquals(KeymapAction.Editing.Basic.Enter, editorResult.getOrElse { null })

        val fileTreeResult = multiContextService.findAction(
            keyCode = KeyEvent.VK_UP, modifiers = 0, context = KeymapContext.FILE_TREE
        )
        assertEquals(KeymapAction.Navigation.Move.Up, fileTreeResult.getOrElse { null })

        val globalFallbackResult = multiContextService.findAction(
            keyCode = KeyEvent.VK_S, modifiers = KeyEvent.CTRL_DOWN_MASK, context = KeymapContext.FILE_TREE
        )
        assertEquals(KeymapAction.File.Save, globalFallbackResult.getOrElse { null })
    }
}